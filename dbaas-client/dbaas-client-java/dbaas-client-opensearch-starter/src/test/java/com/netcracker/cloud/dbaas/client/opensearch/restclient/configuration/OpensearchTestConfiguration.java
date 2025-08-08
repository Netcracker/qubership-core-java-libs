package org.qubership.cloud.dbaas.client.opensearch.restclient.configuration;

import org.qubership.cloud.dbaas.client.DbaasClient;
import org.qubership.cloud.dbaas.client.management.DatabaseConfig;
import org.qubership.cloud.dbaas.client.management.PostConnectProcessor;
import org.qubership.cloud.dbaas.client.metrics.DatabaseMetricProperties;
import org.qubership.cloud.dbaas.client.metrics.DbaaSMetricsRegistrar;
import org.qubership.cloud.dbaas.client.opensearch.config.DbaaSOpensearchConfigurationProperty;
import org.qubership.cloud.dbaas.client.opensearch.config.EnableDbaasOpensearch;
import org.qubership.cloud.dbaas.client.opensearch.config.metrics.TestMicrometerConfiguration;
import org.qubership.cloud.dbaas.client.opensearch.entity.OpensearchDBType;
import org.qubership.cloud.dbaas.client.opensearch.entity.OpensearchIndex;
import org.qubership.cloud.dbaas.client.opensearch.entity.OpensearchIndexConnection;
import org.qubership.cloud.dbaas.client.opensearch.management.OpensearchPostConnectProcessor;
import org.qubership.cloud.dbaas.client.opensearch.DefaultDbaasOpensearchClientBuilderCustomizer;
import org.qubership.cloud.dbaas.client.opensearch.metrics.OpensearchClientRequestsSecondsObservationHandler;
import org.qubership.cloud.dbaas.client.opensearch.metrics.OpensearchMetricsProvider;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.core5.http.HttpHost;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5TransportBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.qubership.cloud.dbaas.client.DbaasConst.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@EnableDbaasOpensearch
@Configuration
@Import(TestMicrometerConfiguration.class)
@Slf4j
public abstract class OpensearchTestConfiguration {

    public static String TEST_PREFIX = "test";
    public static String TEST_INDEX = "opensearch_index";
    public static String TEST_FULL_INDEX_NAME = TEST_PREFIX + "_" + TEST_INDEX;
    public static String TEST_ALIAS = "openserch_alias";

    public static String TEST_TEMPLATE = "opensearch_template";

    @Autowired
    private MeterRegistry meterRegistry;

    @Autowired
    private DbaaSOpensearchConfigurationProperty dbaaSOpensearchConfigurationProperty;

    @Bean
    public OpensearchTestContainer getContainer() {
        OpensearchTestContainer container = OpensearchTestContainer.getInstance();
        container.start();
        try {
            container.execInContainer("curl", "-X", "PUT", "localhost:9200/" + TEST_FULL_INDEX_NAME);
        } catch (IOException | InterruptedException e) {
            log.error("Can't create index in the container");
            throw new RuntimeException(e);
        }
        return container;
    }

    @Bean
    @Primary
    public DbaasClient dbaasClient(OpensearchTestContainer container) {
        DbaasClient dbaasClient = Mockito.mock(DbaasClient.class);
        when(dbaasClient.getOrCreateDatabase(any(), any(), any(), any(DatabaseConfig.class)))
                .thenAnswer((Answer<OpensearchIndex>) invocationOnMock -> {
                    HashMap<String, String> classifierFromMock = (HashMap<String, String>) invocationOnMock.getArguments()[2];
                    DatabaseConfig databaseConfig = (DatabaseConfig) invocationOnMock.getArguments()[3];
                    String databaseName = classifierFromMock.get(SCOPE).equals(SERVICE) ? TEST_INDEX : classifierFromMock.get(TENANT_ID);
                    return getOpensearchIndex(databaseName, container, databaseConfig);
                });
        return dbaasClient;
    }

    public OpensearchIndex getOpensearchIndex(String indexName, OpensearchTestContainer container, DatabaseConfig databaseConfig) {
        OpensearchIndex esIndex = new OpensearchIndex();
        esIndex.setName(indexName);
        esIndex.setClassifier(new TreeMap<>());

        OpensearchIndexConnection connection = new OpensearchIndexConnection();
        String httpHostAddress = container.getHost() + ":" + container.getMappedPort(OpensearchTestContainer.OPENSEARCH_PORT);
        String host = container.getHost();
        Integer port = container.getMappedPort(OpensearchTestContainer.OPENSEARCH_PORT);
        connection.setHost(host);
        connection.setPort(port);
        connection.setUrl("http://" + httpHostAddress);
        connection.setRole("admin");
        if (databaseConfig.getDbNamePrefix() != null) {
            connection.setResourcePrefix(databaseConfig.getDbNamePrefix());
        } else {
            connection.setResourcePrefix(TEST_PREFIX);
        }

        esIndex.setConnectionProperties(connection);

        final ApacheHttpClient5TransportBuilder builder = ApacheHttpClient5TransportBuilder.builder(new HttpHost("http", host, port));

        var opensearchMetricsProvider = new OpensearchMetricsProvider(meterRegistry, dbaaSOpensearchConfigurationProperty.getMetrics());
        var dbaaSMetricsRegistrar = new DbaaSMetricsRegistrar(Collections.singletonList(opensearchMetricsProvider));

        builder.setHttpClientConfigCallback(httpAsyncClientBuilder -> {
            DatabaseMetricProperties metricProperties = DatabaseMetricProperties.builder()
                .databaseName(esIndex.getName())
                .role(esIndex.getConnectionProperties().getRole())
                .classifier(esIndex.getClassifier())
                .extraParameters(Map.of(OpensearchMetricsProvider.HTTP_CLIENT_BUILDER, httpAsyncClientBuilder))
                .additionalTags(Collections.singletonMap(
                    OpensearchClientRequestsSecondsObservationHandler.RESOURCE_PREFIX_TAG_NAME,
                    esIndex.getConnectionProperties().getResourcePrefix()))
                .build();
            dbaaSMetricsRegistrar.registerMetrics(OpensearchDBType.INSTANCE, metricProperties);

            return httpAsyncClientBuilder;
        });

        OpenSearchClient restHighLevelClient = new OpenSearchClient(builder.build());

        connection.setOpenSearchClient(restHighLevelClient);

        return esIndex;
    }

    @Bean
    @Primary
    public PostConnectProcessor opensearchPostConnectProcessor() {
        OpensearchPostConnectProcessor opensearchTransportPostConnectProcessor = spy(new OpensearchPostConnectProcessor(dbaaSOpensearchConfigurationProperty, List.of(new DefaultDbaasOpensearchClientBuilderCustomizer()), null));
        doNothing().when(opensearchTransportPostConnectProcessor).process(any());
        return opensearchTransportPostConnectProcessor;
    }
}
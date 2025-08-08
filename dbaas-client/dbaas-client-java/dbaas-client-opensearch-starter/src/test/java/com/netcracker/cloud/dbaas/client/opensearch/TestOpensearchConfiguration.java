package com.netcracker.cloud.dbaas.client.opensearch;

import org.qubership.cloud.dbaas.client.DbaasClient;
import org.qubership.cloud.dbaas.client.DbaasClientImpl;
import org.qubership.cloud.dbaas.client.management.DatabasePool;
import org.qubership.cloud.dbaas.client.management.PostConnectProcessor;
import org.qubership.cloud.dbaas.client.metrics.DbaaSMetricsRegistrar;
import org.qubership.cloud.dbaas.client.opensearch.config.DbaaSOpensearchConfigurationProperty;
import org.qubership.cloud.dbaas.client.opensearch.config.EnableDbaasOpensearch;
import org.qubership.cloud.dbaas.client.opensearch.entity.DbaasOpensearchMetricsProperties;
import org.qubership.cloud.dbaas.client.opensearch.entity.OpensearchIndex;
import org.qubership.cloud.dbaas.client.opensearch.entity.OpensearchIndexConnection;
import org.qubership.cloud.dbaas.client.opensearch.management.OpensearchPostConnectProcessor;
import org.qubership.cloud.dbaas.client.opensearch.metrics.OpensearchMetricsProvider;
import org.qubership.cloud.restclient.HttpMethod;
import org.qubership.cloud.restclient.MicroserviceRestClient;
import org.qubership.cloud.restclient.entity.RestClientResponseEntity;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.retry.support.RetryTemplate;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@Configuration
@EnableDbaasOpensearch
public class TestOpensearchConfiguration {
    @Bean
    @Primary
    public DbaasClient testDbaasClient(@Autowired(required = false) @Qualifier("dbaasClientDefaultRetryTemplate") RetryTemplate retryTemplate,
                                       @Qualifier("dbaasRestClient") MicroserviceRestClient microserviceRestClient) {
        RestClientResponseEntity<OpensearchIndex> restClientResponseEntity = new RestClientResponseEntity<>(createOpensearchIndex(), HttpStatus.CREATED.value(), null);
        when(microserviceRestClient.doRequest(any(URI.class), eq(HttpMethod.PUT), isNull(), any(), eq(OpensearchIndex.class))).thenReturn(restClientResponseEntity);
        return new DbaasClientImpl(microserviceRestClient, retryTemplate, "http://ms-name.namespace:8080");
    }

    @Bean
    @Primary
    public PostConnectProcessor opensearchPostConnectProcessor() {
        OpensearchMetricsProvider opensearchMetricsProvider = new OpensearchMetricsProvider(
            new SimpleMeterRegistry(), new DbaasOpensearchMetricsProperties()
        );
        DbaaSMetricsRegistrar dbaaSMetricsRegistrar = new DbaaSMetricsRegistrar(Collections.singletonList(opensearchMetricsProvider));

        DbaaSOpensearchConfigurationProperty props = new DbaaSOpensearchConfigurationProperty();
        OpensearchPostConnectProcessor opensearchTransportPostConnectProcessor = spy(new OpensearchPostConnectProcessor(props, Arrays.asList(new DefaultDbaasOpensearchClientBuilderCustomizer()), dbaaSMetricsRegistrar));
        doNothing().when(opensearchTransportPostConnectProcessor).process(any());
        return opensearchTransportPostConnectProcessor;
    }

    protected static OpensearchIndex createOpensearchIndex() {
        OpensearchIndex opensearchIndex = new OpensearchIndex();
        opensearchIndex.setName("index-test-name");
        OpensearchIndexConnection opensearchConnection = new OpensearchIndexConnection();
        opensearchConnection.setHost("test-es-idx-host");
        opensearchIndex.setConnectionProperties(opensearchConnection);
        return opensearchIndex;
    }

    @Primary
    @Bean
    @Qualifier("dbaasRestClient")
    public static MicroserviceRestClient microserviceRestClient() {
        return Mockito.mock(MicroserviceRestClient.class);
    }

    @Primary
    @Bean
    public static DatabasePool databasePool() {
        DatabasePool pool = Mockito.mock(DatabasePool.class);
        when(pool.getOrCreateDatabase(any(), any())).thenReturn(createOpensearchIndex());
        when(pool.getOrCreateDatabase(any(), any(), any())).thenReturn(createOpensearchIndex());
        return pool;
    }
}

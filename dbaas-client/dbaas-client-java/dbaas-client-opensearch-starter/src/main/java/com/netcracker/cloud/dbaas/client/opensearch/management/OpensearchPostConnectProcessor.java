package com.netcracker.cloud.dbaas.client.opensearch.management;

import org.qubership.cloud.dbaas.client.exceptions.DbaasException;
import org.qubership.cloud.dbaas.client.management.PostConnectProcessor;
import org.qubership.cloud.dbaas.client.metrics.DatabaseMetricProperties;
import org.qubership.cloud.dbaas.client.metrics.DbaaSMetricsRegistrar;
import org.qubership.cloud.dbaas.client.opensearch.config.DbaaSOpensearchConfigurationProperty;
import org.qubership.cloud.dbaas.client.opensearch.entity.OpensearchDBType;
import org.qubership.cloud.dbaas.client.opensearch.entity.OpensearchIndex;
import org.qubership.cloud.dbaas.client.opensearch.entity.OpensearchIndexConnection;
import org.qubership.cloud.dbaas.client.opensearch.DbaasOpensearchClientBuilderCustomizer;
import org.qubership.cloud.dbaas.client.opensearch.metrics.OpensearchClientRequestsSecondsObservationHandler;
import org.qubership.cloud.dbaas.client.opensearch.metrics.OpensearchMetricsProvider;
import org.qubership.cloud.security.core.utils.tls.TlsUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.CredentialsProvider;
import org.apache.hc.client5.http.auth.CredentialsStore;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.impl.async.HttpAsyncClientBuilder;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManager;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.ClientTlsStrategyBuilder;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.nio.ssl.TlsStrategy;
import org.apache.hc.core5.reactor.ssl.TlsDetails;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.transport.OpenSearchTransport;
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5TransportBuilder;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
public class OpensearchPostConnectProcessor implements PostConnectProcessor<OpensearchIndex> {

    private List<DbaasOpensearchClientBuilderCustomizer> builderCustomizers;

    private DbaaSOpensearchConfigurationProperty configurationProperties;
    private DbaaSMetricsRegistrar metricsRegistrar;

    public OpensearchPostConnectProcessor(DbaaSOpensearchConfigurationProperty configurationProperties,
                                          List<DbaasOpensearchClientBuilderCustomizer> builderCustomizers) {
        this.configurationProperties = configurationProperties;
        this.builderCustomizers = builderCustomizers;
    }

    public OpensearchPostConnectProcessor(DbaaSOpensearchConfigurationProperty configurationProperties,
                                          List<DbaasOpensearchClientBuilderCustomizer> builderCustomizers, DbaaSMetricsRegistrar metricsRegistrar) {
        this.configurationProperties = configurationProperties;
        this.builderCustomizers = builderCustomizers;
        this.metricsRegistrar = metricsRegistrar;
    }

    @Override
    public void process(OpensearchIndex database) {
        Objects.requireNonNull(database, "Can't process null database");
        String host = database.getConnectionProperties().getHost();
        int port = database.getConnectionProperties().getPort();
        boolean tlsSupported = database.getConnectionProperties().isTls();
        String url = buildSSLUrl(database.getConnectionProperties().getUrl(), tlsSupported);
        database.getConnectionProperties().setUrl(url);
        String proto;
        try {
            proto = new URL(url).getProtocol();
        } catch (MalformedURLException e) {
            log.error("Error while parse url of the created index: {}", database);
            throw new RuntimeException(e);
        }

        HttpHost httpHost = new HttpHost(proto, host, port);
        ApacheHttpClient5TransportBuilder transportBuilder = ApacheHttpClient5TransportBuilder.builder(httpHost);
        transportBuilder.setHttpClientConfigCallback(httpClientBuilder -> {
            builderCustomizers.forEach(customizer -> customizer.customize(httpClientBuilder));
            TlsStrategy tlsStrategy = ClientTlsStrategyBuilder.create()
                    .setSslContext(TlsUtils.getSslContext())
                    .setTlsDetailsFactory(sslEngine -> new TlsDetails(sslEngine.getSession(), sslEngine.getApplicationProtocol()))
                    .build();

            PoolingAsyncClientConnectionManagerBuilder connectionManagerBuilder = PoolingAsyncClientConnectionManagerBuilder.create()
                    .setTlsStrategy(tlsStrategy);
            if (configurationProperties.getMaxConnTotal() != null)
                connectionManagerBuilder.setMaxConnTotal(configurationProperties.getMaxConnTotal());
            if (configurationProperties.getMaxConnPerRoute() != null)
                connectionManagerBuilder.setMaxConnTotal(configurationProperties.getMaxConnPerRoute());

            builderCustomizers.forEach(customizer -> customizer.customize(connectionManagerBuilder));

            PoolingAsyncClientConnectionManager connectionManager = connectionManagerBuilder.build();

            registerMetrics(database, httpClientBuilder);

            return httpClientBuilder
                    .setDefaultCredentialsProvider(buildCredentialsProvider(database.getConnectionProperties(), httpHost))
                    .setConnectionManager(connectionManager);
        });
        transportBuilder.setRequestConfigCallback(requestConfigBuilder -> {
            builderCustomizers.forEach(customizer -> customizer.customize(requestConfigBuilder));
            return requestConfigBuilder;
        });

        builderCustomizers.forEach(customizer -> customizer.customize(transportBuilder));
        OpenSearchTransport transport = transportBuilder.build();
        OpenSearchClient client = new OpenSearchClient(transport);
        database.getConnectionProperties().setOpenSearchClient(client);
    }

    private String buildSSLUrl(String url, boolean tlsSupported) {
        if (tlsSupported && !url.startsWith("https")) {
            log.warn("TLS is requested for opensearch, but URL is not secured. Will update protocol to HTTPS");
            url = url.replace("http", "https");
        }
        if (!tlsSupported && url.startsWith("https")) {
            log.warn("TLS for opensearch is disabled in client, but URL is secured. Will update protocol to HTTP");
            url = url.replace("https", "http");
        }
        return url;
    }

    private void registerMetrics(OpensearchIndex database, HttpAsyncClientBuilder httpClientBuilder) {
        if (metricsRegistrar != null) {
            DatabaseMetricProperties metricProperties = DatabaseMetricProperties.builder()
                .databaseName(database.getName())
                .role(database.getConnectionProperties().getRole())
                .classifier(database.getClassifier())
                .extraParameters(Map.of(OpensearchMetricsProvider.HTTP_CLIENT_BUILDER, httpClientBuilder))
                .additionalTags(Collections.singletonMap(
                    OpensearchClientRequestsSecondsObservationHandler.RESOURCE_PREFIX_TAG_NAME,
                    database.getConnectionProperties().getResourcePrefix()))
                .build();
            metricsRegistrar.registerMetrics(OpensearchDBType.INSTANCE, metricProperties);
        }
    }

    private CredentialsProvider buildCredentialsProvider(OpensearchIndexConnection connection, HttpHost httpHost) {
        String username = "";
        String password;
       if (isCredentialsNotEmpty(connection.getUsername(), connection.getPassword())) {
            password = connection.getPassword();
            username = connection.getUsername();
            log.debug("use password from connection properties");
        } else {
            String message = "Failed to get database username and password. " +
                    "Provide global parameters or update dbaas and opensearch-adapter version";
            log.error(message);
            throw new DbaasException(message);
        }
        final CredentialsStore credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(new AuthScope(httpHost), new UsernamePasswordCredentials(username, password.toCharArray()));
        return credentialsProvider;
    }

    private boolean isCredentialsNotEmpty(String username, String password) {
        return isNotEmpty(username) && isNotEmpty(password);
    }

    boolean isNotEmpty(String string) {
        return string != null && !string.isBlank();
    }

    @Override
    public Class<OpensearchIndex> getSupportedDatabaseType() {
        return OpensearchIndex.class;
    }
}

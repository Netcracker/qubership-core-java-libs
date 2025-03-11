package org.qubership.cloud.quarkus.dbaas.opensearch.client.service.impl;

import org.qubership.cloud.dbaas.client.DbaasClient;
import org.qubership.cloud.dbaas.client.entity.connection.DatabaseConnection;
import org.qubership.cloud.dbaas.client.management.DatabaseConfig;
import org.qubership.cloud.dbaas.client.management.DbaasDbClassifier;
import org.qubership.cloud.dbaas.client.metrics.DatabaseMetricProperties;
import org.qubership.cloud.dbaas.client.metrics.DbaaSMetricsRegistrar;
import org.qubership.cloud.dbaas.client.opensearch.entity.OpensearchDBType;
import org.qubership.cloud.dbaas.client.opensearch.entity.OpensearchDatabaseSettings;
import org.qubership.cloud.dbaas.client.opensearch.entity.OpensearchIndex;
import org.qubership.cloud.dbaas.client.opensearch.entity.OpensearchIndexConnection;
import org.qubership.cloud.dbaas.client.opensearch.metrics.OpensearchClientRequestsSecondsObservationHandler;
import org.qubership.cloud.dbaas.client.opensearch.metrics.OpensearchMetricsProvider;
import org.qubership.cloud.dbaas.client.opensearch.service.OpensearchLogicalDbProvider;
import org.qubership.cloud.quarkus.dbaas.opensearch.client.config.DbaaSOpensearchConfigurationProperty;
import org.qubership.cloud.quarkus.dbaas.opensearch.client.config.DbaaSOpensearchCreationConfig;
import org.qubership.cloud.quarkus.dbaas.opensearch.client.config.OpensearchConfiguration;
import org.qubership.cloud.quarkus.dbaas.opensearch.client.service.OpensearchDbaaSApiClient;
import jakarta.enterprise.inject.Instance;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.CredentialsProvider;
import org.apache.hc.client5.http.auth.CredentialsStore;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.impl.async.HttpAsyncClientBuilder;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.ClientTlsStrategyBuilder;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.nio.ssl.TlsStrategy;
import org.apache.hc.core5.reactor.ssl.TlsDetails;
import org.jetbrains.annotations.NotNull;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.transport.OpenSearchTransport;
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5TransportBuilder;
import org.qubership.cloud.security.core.utils.tls.TlsUtils;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static org.qubership.cloud.dbaas.client.DbaasConst.SCOPE;
import static org.qubership.cloud.dbaas.client.DbaasConst.TENANT;
import static org.qubership.cloud.dbaas.client.DbaasConst.TENANT_ID;

@Slf4j
public class OpensearchDbaaSApiClientImpl implements OpensearchDbaaSApiClient {
    private String namespace;
    private Instance<OpensearchLogicalDbProvider> dbProviders;
    private DbaaSOpensearchCreationConfig opensearchCreationConfig;
    private DbaasClient dbaaSClient;
    private final Map<DbaasDbClassifier, OpensearchIndex> opensearchIdxMap = new ConcurrentHashMap<>();
    private DbaaSOpensearchConfigurationProperty configurationProperty;
    private DbaaSMetricsRegistrar metricsRegistrar;

    public OpensearchDbaaSApiClientImpl(String namespace,
                                        Instance<OpensearchLogicalDbProvider> dbProviders,
                                        DbaaSOpensearchCreationConfig opensearchCreationConfig,
                                        DbaasClient dbaaSClient,
                                        DbaaSOpensearchConfigurationProperty configurationProperty) {
        this.namespace = namespace;
        this.dbProviders = dbProviders;
        this.opensearchCreationConfig = opensearchCreationConfig;
        this.dbaaSClient = dbaaSClient;
        this.configurationProperty = configurationProperty;
    }

    public OpensearchDbaaSApiClientImpl(String namespace,
                                        Instance<OpensearchLogicalDbProvider> dbProviders,
                                        DbaaSOpensearchCreationConfig opensearchCreationConfig,
                                        DbaasClient dbaaSClient,
                                        DbaaSOpensearchConfigurationProperty configurationProperty,
                                        DbaaSMetricsRegistrar metricsRegistrar) {
        this.namespace = namespace;
        this.dbProviders = dbProviders;
        this.opensearchCreationConfig = opensearchCreationConfig;
        this.dbaaSClient = dbaaSClient;
        this.configurationProperty = configurationProperty;
        this.metricsRegistrar = metricsRegistrar;
    }

    @Override
    public OpensearchIndexConnection getOrCreateOpensearchIndex(DbaasDbClassifier classifier) {
        log.trace("Create new opensearch database for {}", classifier);
        return opensearchIdxMap.computeIfAbsent(classifier, this::createIndex).getConnectionProperties();
    }

    @Override
    public DatabaseConnection getOpensearchIndex(DbaasDbClassifier classifier) {
        return dbaaSClient.getConnection(OpensearchDBType.INSTANCE, namespace, null, classifier.asMap());
    }

    @Override
    public OpensearchIndexConnection getOrCreateOpensearchIndex(DatabaseConfig databaseConfig, DbaasDbClassifier classifier) {
        log.trace("Create new opensearch database for {}", classifier);
        return opensearchIdxMap.computeIfAbsent(classifier,
                dbClassifier -> createIndex(databaseConfig, dbClassifier)).getConnectionProperties();
    }

    @Override
    public void removeCachedDatabase(DbaasDbClassifier classifier) {
        log.trace("removing cached database for {}", classifier);
        opensearchIdxMap.remove(classifier);
    }

    /**
     * In this method all databaseConfig parameters specify by user.
     * Exclusion is mandatory fields such as resourcePrefix, createOnly and dbNamePrefix.
     */
    private OpensearchIndex createIndex(DatabaseConfig databaseConfig, DbaasDbClassifier dbClassifier) {
        Objects.requireNonNull(databaseConfig, "dbCreateParameters should not be null");
        databaseConfig.setDatabaseSettings(
                updateDatabaseSettings((OpensearchDatabaseSettings) databaseConfig.getDatabaseSettings()));
        TreeMap<String, Object> classifier = new TreeMap<>(dbClassifier.asMap());
        databaseConfig.setDbNamePrefix(getDatabasePrefix(databaseConfig, classifier));
        return getDatabaseFromProviders(classifier, databaseConfig);
    }


    private OpensearchIndex createIndex(DbaasDbClassifier dbaasDbClassifier) {
        Map<String, Object> classifier = dbaasDbClassifier.asMap();
        log.debug("Create new Opensearch User for {}", classifier);

        OpensearchIndex db = getDatabase(classifier);
        log.debug("Connection: " + db.getConnectionProperties());

        log.debug("Starting the initialization of OpensearchClient for database with classifier: {}", db.getClassifier());
        OpensearchIndexConnection connectionProperties = db.getConnectionProperties();

        OpenSearchClient openSearchClient = getOpenSearchClient(db);
        connectionProperties.setOpenSearchClient(openSearchClient);
        return db;
    }

    private OpensearchIndex getDatabase(Map<String, Object> dbClassifier) {
        TreeMap<String, Object> classifier = new TreeMap<>(dbClassifier);
        DatabaseConfig databaseConfig = getDatabaseConfig(classifier);
        return getDatabaseFromProviders(classifier, databaseConfig);
    }

    private DatabaseConfig getDatabaseConfig(Map<String, Object> classifier) {
        log.debug("Create DbParameters for database with classifier {}", classifier);
        String tenantId = (String) classifier.get("tenantId");
        OpensearchConfiguration dbConfiguration = opensearchCreationConfig.getOpensearchConfiguration(tenantId);
        DatabaseConfig.Builder builder = DatabaseConfig.builder();

        if (dbConfiguration != null) {
            builder.physicalDatabaseId(dbConfiguration.physicalDatabaseId.orElse(null));
        }
        opensearchCreationConfig.runtimeUserRole.ifPresent(builder::userRole);

        builder.databaseSettings(updateDatabaseSettings(null));
        DatabaseConfig databaseConfig = builder.build();

        databaseConfig.setDbNamePrefix(getDatabasePrefix(databaseConfig, classifier));
        return databaseConfig;
    }

    @NotNull
    private OpenSearchClient getOpenSearchClient(OpensearchIndex db) {
        OpensearchIndexConnection connectionProperties = db.getConnectionProperties();
        String host = connectionProperties.getHost();
        int port = connectionProperties.getPort();
        boolean tlsSupported = connectionProperties.isTls();
        String url = connectionProperties.getUrl();
        connectionProperties.setUrl(buildSSLUrl(url, tlsSupported));
        String proto;
        try {
            proto = new URL(connectionProperties.getUrl()).getProtocol();
        } catch (MalformedURLException e) {
            log.error("Error while parse url of the created index: {}", db);
            throw new RuntimeException(e);
        }

        HttpHost httpHost = new HttpHost(proto, host, port);
        ApacheHttpClient5TransportBuilder transportBuilder = ApacheHttpClient5TransportBuilder.builder(httpHost);
        transportBuilder.setHttpClientConfigCallback(httpClientBuilder -> {
            TlsStrategy tlsStrategy = ClientTlsStrategyBuilder.create()
                    .setSslContext(TlsUtils.getSslContext())
                    .setTlsDetailsFactory(sslEngine -> new TlsDetails(sslEngine.getSession(), sslEngine.getApplicationProtocol()))
                    .build();

            PoolingAsyncClientConnectionManagerBuilder connectionManagerBuilder = PoolingAsyncClientConnectionManagerBuilder
                    .create()
                    .setTlsStrategy(tlsStrategy);
            configurationProperty.maxConnTotal.ifPresent(connectionManagerBuilder::setMaxConnTotal);
            configurationProperty.maxConnPerRoute.ifPresent(connectionManagerBuilder::setMaxConnPerRoute);

            registerMetrics(db, httpClientBuilder);

            return httpClientBuilder
                    .setDefaultCredentialsProvider(buildCredentialsProvider(db.getConnectionProperties(), httpHost))
                    .setConnectionManager(connectionManagerBuilder.build());
        });
        OpenSearchTransport transport = transportBuilder.build();
        return new OpenSearchClient(transport);
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

    private CredentialsProvider buildCredentialsProvider(OpensearchIndexConnection connection, HttpHost httpHost) {
        String username = connection.getUsername();
        String dbPassword = connection.getPassword();
        final CredentialsStore credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(new AuthScope(httpHost),
                new UsernamePasswordCredentials(username, dbPassword.toCharArray()));
        return credentialsProvider;
    }

    @NotNull
    private OpensearchIndex getDatabaseFromProviders(TreeMap<String, Object> classifier, DatabaseConfig databaseConfig) {
        for (OpensearchLogicalDbProvider logicalDbProvider : sortProviders(dbProviders)) {
            OpensearchIndex database = logicalDbProvider.provide(classifier, databaseConfig, namespace);
            if (database != null) {
                if (database.getConnectionProperties() == null) {
                    throw new IllegalStateException("Provider: " + logicalDbProvider + "have provided opensearch database " +
                            "but connection properties is null");
                }
                return database;
            }
        }
        throw new IllegalStateException("Not one of the providers: " + dbProviders + " could provide a logical Opensearch database");
    }

    private OpensearchDatabaseSettings updateDatabaseSettings(OpensearchDatabaseSettings databaseSettings) {
        if (databaseSettings == null) {
            databaseSettings = new OpensearchDatabaseSettings();
        }
        databaseSettings.setResourcePrefix(true);
        databaseSettings.setCreateOnly(Collections.singletonList("user"));
        return databaseSettings;
    }

    private String getDatabasePrefix(DatabaseConfig databaseConfig, Map<String, Object> classifier) {
        if (opensearchCreationConfig != null && databaseConfig.getDbNamePrefix() == null) {
            if (opensearchCreationConfig.singleTeantDbConfig.prefixConfig != null && opensearchCreationConfig.singleTeantDbConfig.prefixConfig.getPrefix().isPresent()
                    && classifier.get(SCOPE) == TENANT) {
                return opensearchCreationConfig.singleTeantDbConfig.prefixConfig.getPrefix().get()
                        .replace("{tenantId}", (String) classifier.get(TENANT_ID));

            } else if (opensearchCreationConfig.serviceDbConfiguration.prefixConfig != null
                    && opensearchCreationConfig.serviceDbConfiguration.prefixConfig.getPrefix().isPresent()) {
                return opensearchCreationConfig.serviceDbConfiguration.prefixConfig.getPrefix().get();
            }
        }
        return databaseConfig.getDbNamePrefix();
    }

    private List<OpensearchLogicalDbProvider> sortProviders(Instance<OpensearchLogicalDbProvider> dbProviders) {
        return dbProviders.stream().sorted(Comparator.comparingInt(OpensearchLogicalDbProvider::order)).collect(Collectors.toList());
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
}

package org.qubership.cloud.core.quarkus.dbaas.datasource.service.impl;

import com.google.common.base.Strings;
import org.qubership.cloud.core.quarkus.dbaas.datasource.config.AgroalConnectionPoolConfigurationFactory;
import org.qubership.cloud.core.quarkus.dbaas.datasource.config.DbaasDatasourcePoolConfiguration;
import org.qubership.cloud.core.quarkus.dbaas.datasource.config.flyway.CoreFlywayConfig;
import org.qubership.cloud.core.quarkus.dbaas.datasource.config.properties.DbaaSPostgresDbCreationConfig;
import org.qubership.cloud.core.quarkus.dbaas.datasource.config.properties.PostgresDbConfiguration;
import org.qubership.cloud.core.quarkus.dbaas.datasource.service.DbaaSPostgresDbCreationService;
import org.qubership.cloud.core.quarkus.dbaas.datasource.service.MigrationService;
import org.qubership.cloud.core.quarkus.dbaas.datasource.service.agroal.AgroalConnectionFactoryConfigurationBuilder;
import org.qubership.cloud.core.quarkus.dbaas.datasource.service.agroal.auth.DbaasSecurityProvider;
import org.qubership.cloud.dbaas.client.entity.database.DatasourceConnectorSettings;
import org.qubership.cloud.dbaas.client.entity.database.PostgresDatabase;
import org.qubership.cloud.dbaas.client.entity.database.type.PostgresDBType;
import org.qubership.cloud.dbaas.client.management.DatabaseConfig;
import org.qubership.cloud.dbaas.client.management.DbaasDbClassifier;
import org.qubership.cloud.dbaas.client.metrics.DatabaseMetricProperties;
import org.qubership.cloud.dbaas.client.metrics.DbaaSMetricsRegistrar;
import org.qubership.cloud.dbaas.client.service.PostgresqlLogicalDbProvider;
import org.qubership.cloud.dbaas.client.service.flyway.FlywayContext;
import io.agroal.api.AgroalDataSource;
import io.agroal.api.configuration.AgroalConnectionFactoryConfiguration;
import io.agroal.api.configuration.supplier.AgroalConnectionFactoryConfigurationSupplier;
import io.agroal.api.configuration.supplier.AgroalConnectionPoolConfigurationSupplier;
import io.agroal.api.configuration.supplier.AgroalDataSourceConfigurationSupplier;
import io.agroal.api.security.AgroalSecurityProvider;
import io.agroal.api.security.NamePrincipal;
import io.agroal.api.security.SimplePassword;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static org.qubership.cloud.core.quarkus.dbaas.datasource.metrics.PostgresMicrometerMetricsProvider.DATASOURCE_PARAMETER;
import static org.qubership.cloud.core.quarkus.dbaas.datasource.metrics.PostgresMicrometerMetricsProvider.SCHEMA_TAG;
import static org.qubership.cloud.dbaas.client.DbaasConst.*;

@Slf4j
@ApplicationScoped
public class DbaaSPostgresDbCreationServiceImpl implements DbaaSPostgresDbCreationService {
    public static final String SSL_FACTORY_POSTFIX = "sslfactory=org.postgresql.ssl.DefaultJavaSSLFactory";
    public static final String SSL_MODE_VERIFY_FULL_POSTFIX = "sslmode=verify-full";
    public static final String SSL_MODE_REQUIRE_POSTFIX = "sslmode=require";
    public static final String CURRENT_SCHEMA_PARAM = "currentSchema=";
    private final Map<DatabaseKey, PostgresDatabase> postgresDbMap = new ConcurrentHashMap<>();


    private String namespace;

    private MigrationService migrationService;

    private CoreFlywayConfig coreFlywayConfig;

    private AgroalConnectionPoolConfigurationFactory connectionPoolConfigurationFactory;

    private AgroalConnectionFactoryConfiguration connectionFactoryConfiguration;

    private DbaaSPostgresDbCreationConfig postgresDbConfiguration;

    private DbaasDatasourcePoolConfiguration dbaasPoolConfiguration;

    private List<PostgresqlLogicalDbProvider> dbProviders;

    private DatasourceConnectorSettings defaultConnectorSettings;

    private DbaaSMetricsRegistrar metricsRegistrar;

    public DbaaSPostgresDbCreationServiceImpl(@ConfigProperty(name = "cloud.microservice.namespace")
                                              String namespace,
                                              MigrationService migrationService,
                                              CoreFlywayConfig coreFlywayConfig,
                                              AgroalConnectionPoolConfigurationFactory connectionPoolConfigurationFactory,
                                              AgroalConnectionFactoryConfiguration connectionFactoryConfiguration,
                                              DbaaSPostgresDbCreationConfig postgresDbConfiguration,
                                              DbaasDatasourcePoolConfiguration dbaasPoolConfiguration,
                                              Instance<PostgresqlLogicalDbProvider> dbProviders,
                                              DbaaSMetricsRegistrar metricsRegistrar) {
        this.namespace = namespace;
        this.migrationService = migrationService;
        this.coreFlywayConfig = coreFlywayConfig;
        this.connectionPoolConfigurationFactory = connectionPoolConfigurationFactory;
        this.connectionFactoryConfiguration = connectionFactoryConfiguration;
        this.postgresDbConfiguration = postgresDbConfiguration;
        this.dbaasPoolConfiguration = dbaasPoolConfiguration;
        this.dbProviders = sortProviders(dbProviders);
        this.defaultConnectorSettings = new DatasourceConnectorSettings();
        this.metricsRegistrar = metricsRegistrar;
    }

    private List<PostgresqlLogicalDbProvider> sortProviders(Instance<PostgresqlLogicalDbProvider> dbProviders) {
        return dbProviders.stream().sorted(Comparator.comparingInt(PostgresqlLogicalDbProvider::order)).collect(Collectors.toList());
    }

    @Override
    public PostgresDatabase getOrCreatePostgresDatabase(DbaasDbClassifier classifier) {
        log.trace("Create new postgres database for {}", classifier);
        return getOrCreatePostgresDatabase(classifier, defaultConnectorSettings, null);
    }

    @Override
    public PostgresDatabase getOrCreatePostgresDatabase(DbaasDbClassifier classifier, DatasourceConnectorSettings connectorSettings, DatabaseConfig databaseConfig) {
        log.trace("Create new postgres database for {}", classifier);
        DatabaseKey key = new DatabaseKey(classifier.asMap(), connectorSettings.getDiscriminator() == null ? null : connectorSettings.getDiscriminator().getValue());
        return postgresDbMap.computeIfAbsent(key, k -> createPostgresDatabase(classifier, connectorSettings, databaseConfig));
    }

    @Override
    public void updatePostgresDatabasesPasswords(DbaasDbClassifier classifier) {
        updatePostgresDatabasesPasswords(classifier, new DatasourceConnectorSettings(), null);
    }

    @Override
    public void updatePostgresDatabasesPasswords(DbaasDbClassifier classifier, DatasourceConnectorSettings connectorSettings, DatabaseConfig databaseConfig) {
        DatabaseKey key = new DatabaseKey(classifier.asMap(), connectorSettings.getDiscriminator() == null ? null : connectorSettings.getDiscriminator().getValue());
        postgresDbMap.compute(key, (dbKey, existedDb) -> {
            PostgresDatabase newDb = getPostgresDatabase(dbKey.getClassifier(), databaseConfig);
            String password = extractPassword(newDb);
            updateDbPassword(existedDb.getConnectionProperties().getDataSource(), password);
            log.info("DB passwords were updated successfully");
            return existedDb;
        });
    }

    private void updateDbPassword(DataSource dataSource, String password) {
        AgroalSecurityProvider securityProvider = getSecurityProvider((AgroalDataSource) dataSource);

        if (securityProvider instanceof DbaasSecurityProvider) {
            DbaasSecurityProvider dbaasSecurityProvider = (DbaasSecurityProvider) securityProvider;
            dbaasSecurityProvider.setCurrentDatabasePassword(new SimplePassword(password));
        } else {
            throw new IllegalStateException("Cannot update db password because DbaasSecurityProvider not enabled");
        }
    }

    private PostgresDatabase createPostgresDatabase(DbaasDbClassifier classifier, DatasourceConnectorSettings connectorSettings, DatabaseConfig databaseConfig) {

        Map<String, Object> dbClassifier = classifier.asMap();
        PostgresDatabase db = getPostgresDatabase(dbClassifier, databaseConfig);
        String dbPassword = extractPassword(db);
        log.debug("Connection: " + db.getConnectionProperties());
        String logicalDbName = db.isClassifierContainsLogicalDbName() ?
                (String) ((Map<String, Object>) dbClassifier.get(CUSTOM_KEYS)).get(LOGICAL_DB_NAME) : null;

        String url = db.getConnectionProperties().getUrl();
        List<String> additionalUrlParams = new ArrayList<>();

        if (db.getConnectionProperties().isTls()) {
            if(db.getConnectionProperties().isTlsNotStrict())
                additionalUrlParams.add(SSL_MODE_REQUIRE_POSTFIX);
            else
                additionalUrlParams.add(SSL_MODE_VERIFY_FULL_POSTFIX);
            additionalUrlParams.add(SSL_FACTORY_POSTFIX);
        }
        String schema = connectorSettings.getSchema();
        if (Strings.isNullOrEmpty(schema)) {
            Properties jdbcProperties = dbaasPoolConfiguration.getJdbcProperties(logicalDbName);
            schema = jdbcProperties.getProperty("currentSchema");
            if (!Strings.isNullOrEmpty(schema)) {
                connectorSettings.setSchema(schema);
                log.debug("Schema set from JDBC properties: {}", schema);
            }
        }
        if (!Strings.isNullOrEmpty(schema)) {
            additionalUrlParams.add(CURRENT_SCHEMA_PARAM + schema);
        }
        if (!additionalUrlParams.isEmpty()) {
            url = appendUrlParams(url, additionalUrlParams);
        }
        log.debug("Target connection url={}", url);
        AgroalDataSource dataSource = createDatasource(url,
                db.getConnectionProperties().getUsername(),
                dbPassword, logicalDbName, connectorSettings);

        db.getConnectionProperties().setDataSource(dataSource);
        boolean isMigratedWithFactory = false;
        if (connectorSettings.getFlywayRunner() != null) {
            isMigratedWithFactory = true;
            log.info("going to execute flyway migrations for {}", classifier);
            connectorSettings.getFlywayRunner().run(new FlywayContext(dataSource));
        }
        try {
            if (!isMigratedWithFactory && (isTenantDb(dbClassifier) || !isStartTimeMigration())) {
                migrationService.migrate(dataSource, logicalDbName);
            }
            log.debug("Database {} was created successfully", db);
        } catch (Exception e) {
            log.error("Cannot perform migration for {}", db, e);
            dataSource.close();
            throw e;
        }
        registerMetrics(db, connectorSettings);
        return db;
    }

    private String appendUrlParams(String url, List<String> params) {
        String paramsString = String.join("&", params);
        String concatSign = "&";
        if (!url.contains("?")) {
            concatSign = "?";
        }
        return url + concatSign + paramsString;
    }

    private String extractPassword(PostgresDatabase db) {
        return db.getConnectionProperties().getPassword();
    }

    @NotNull
    private PostgresDatabase getPostgresDatabase(Map<String, Object> dbClassifier, DatabaseConfig databaseConfig) {
        TreeMap<String, Object> classifier = new TreeMap<>(dbClassifier);
        DatabaseConfig config = (databaseConfig == null) ? getDbCreateParameters(dbClassifier) : databaseConfig;
        for (PostgresqlLogicalDbProvider dbProvider : dbProviders) {
            PostgresDatabase postgresDatabase = dbProvider.provide(classifier, config, namespace);
            if (postgresDatabase != null) {
                if (postgresDatabase.getConnectionProperties() == null) {
                    throw new IllegalStateException("Provider: " + dbProvider + "have provided postgresql database " +
                            "but connection properties is null");
                }
                return postgresDatabase;
            }
        }
        throw new IllegalStateException("Not one of the providers: " + dbProviders + " could provide a logical postgresql database");
    }

    private AgroalDataSource createDatasource(String url, String username, String password, @Nullable String logicalDbName, DatasourceConnectorSettings connectorSettings) {
        Map<String, Object> connectionParams = connectorSettings.getConnPropertiesParam();
        AgroalConnectionPoolConfigurationSupplier dbaasConnectionPool = new AgroalConnectionPoolConfigurationSupplier(connectionPoolConfigurationFactory.createAgroalConnectionPoolConfiguration(logicalDbName, connectionParams));
        SimplePassword securityPassword = new SimplePassword(password);
        Properties jdbcProperties = dbaasPoolConfiguration.getJdbcProperties(logicalDbName);
        Properties xaProperties = dbaasPoolConfiguration.getXaProperties(logicalDbName);
        removeJdbcPrefixedProperties(connectionParams);
        if (connectionParams != null && !connectionParams.isEmpty()) {
            jdbcProperties.putAll(connectionParams);
        }

        AgroalConnectionFactoryConfigurationBuilder connectionFactoryConfigurationBuilder = new AgroalConnectionFactoryConfigurationBuilder()
                .jdbcUrl(url)
                .jdbcProperties(jdbcProperties)
                .xaProperties(xaProperties)
                .principal(new NamePrincipal(username))
                .credential(securityPassword)
                .securityProvider(new DbaasSecurityProvider(securityPassword))
                .autoCommit(connectionFactoryConfiguration.autoCommit());

        if (dbaasPoolConfiguration.isXa(logicalDbName)) {
            connectionFactoryConfigurationBuilder.connectionProviderClass(org.postgresql.xa.PGXADataSource.class);
        }

        AgroalConnectionFactoryConfigurationSupplier connectionFactory = new AgroalConnectionFactoryConfigurationSupplier(
                connectionFactoryConfigurationBuilder.build());
        dbaasConnectionPool.connectionFactoryConfiguration(connectionFactory);

        AgroalDataSourceConfigurationSupplier configurationSupplier = new AgroalDataSourceConfigurationSupplier()
                .connectionPoolConfiguration(dbaasConnectionPool);

        configurationSupplier.metricsEnabled(true);
        try {
            return dbaasPoolConfiguration.getDatasourceProperties().debugDatasourceListeners ?
                    AgroalDataSource.from(configurationSupplier, new DebugDatasourceListener()) :
                    AgroalDataSource.from(configurationSupplier);
        } catch (SQLException ex) {
            log.error("Got error during creating agroalDataSource with url {} and username {}", url, username);
            throw new RuntimeException(ex);
        }
    }

    private DatabaseConfig getDbCreateParameters(Map<String, Object> classifier) {
        log.debug("Create DbParameters for database with classifier {}", classifier);
        String tenantId = (String) classifier.get("tenantId");
        PostgresDbConfiguration dbConfiguration = postgresDbConfiguration.getPostgresDbConfiguration(tenantId);
        DatabaseConfig.Builder builder = DatabaseConfig.builder();
        if (dbConfiguration != null) {
            builder.physicalDatabaseId(dbConfiguration.physicalDatabaseId.orElse(null));
            dbConfiguration.databaseSettings.ifPresent(builder::databaseSettings);
        }
        builder.userRole(postgresDbConfiguration.dbaasApiPropertiesConfig.getDbaaseApiProperties().getRuntimeUserRole());
        builder.dbNamePrefix(postgresDbConfiguration.dbaasApiPropertiesConfig.getDbaaseApiProperties().getDbPrefix());
        return builder.build();
    }

    private boolean isStartTimeMigration() {
        return coreFlywayConfig.globalFlywayConfig.cleanAndMigrateAtStart;
    }

    private boolean isTenantDb(Map<String, Object> classifier) {
        return classifier.containsKey(TENANT_ID);
    }

    private AgroalSecurityProvider getSecurityProvider(AgroalDataSource dataSource) {
        return dataSource.getConfiguration()
                .connectionPoolConfiguration()
                .connectionFactoryConfiguration()
                .securityProviders()
                .stream()
                .findFirst()
                .get();
    }

    private void registerMetrics(PostgresDatabase database, DatasourceConnectorSettings settings) {
        if (metricsRegistrar != null) {
            if (database.getName() == null) {
                log.warn("Database name is null");
            }
            DatabaseMetricProperties metricProperties = DatabaseMetricProperties.builder()
                    .databaseName(database.getName())
                    .role(database.getConnectionProperties().getRole())
                    .classifier(database.getClassifier())
                    .extraParameters(Map.of(DATASOURCE_PARAMETER, database.getConnectionProperties().getDataSource()))
                    .additionalTags(Map.of(SCHEMA_TAG, String.valueOf(settings.getSchema())))
                    .build();
            metricsRegistrar.registerMetrics(PostgresDBType.INSTANCE, metricProperties);
        }
    }

    private void removeJdbcPrefixedProperties(Map<String, Object> connectionParams) {
        if (connectionParams != null && !connectionParams.isEmpty()) {
            Iterator<Map.Entry<String, Object>> it = connectionParams.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, Object> entry = it.next();
                if (entry.getKey().startsWith(AgroalConnectionPoolConfigurationFactory.JDBC_PROPERTY_PREFIX)) {
                    it.remove();
                }
            }
        }
    }
}

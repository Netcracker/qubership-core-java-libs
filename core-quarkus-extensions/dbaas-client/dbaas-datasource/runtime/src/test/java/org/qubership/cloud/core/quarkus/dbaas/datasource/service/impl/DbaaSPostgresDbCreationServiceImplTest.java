package org.qubership.cloud.core.quarkus.dbaas.datasource.service.impl;

import org.qubership.cloud.core.quarkus.dbaas.datasource.config.AgroalConnectionPoolConfigurationFactory;
import org.qubership.cloud.core.quarkus.dbaas.datasource.config.DbaasDatasourcePoolConfiguration;
import org.qubership.cloud.core.quarkus.dbaas.datasource.config.flyway.CoreFlywayConfig;
import org.qubership.cloud.core.quarkus.dbaas.datasource.config.flyway.FlywayConfig;
import org.qubership.cloud.core.quarkus.dbaas.datasource.config.properties.DatasourceProperties;
import org.qubership.cloud.core.quarkus.dbaas.datasource.config.properties.DbaaSPostgresDbCreationConfig;
import org.qubership.cloud.core.quarkus.dbaas.datasource.config.properties.JDBCConfig;
import org.qubership.cloud.core.quarkus.dbaas.datasource.config.properties.PostgresDbConfiguration;
import org.qubership.cloud.core.quarkus.dbaas.datasource.service.MigrationService;
import org.qubership.cloud.core.quarkus.dbaas.datasource.service.agroal.AgroalConnectionFactoryConfigurationBuilder;
import org.qubership.cloud.core.quarkus.dbaas.datasource.service.agroal.auth.DbaasSecurityProvider;
import org.qubership.cloud.core.quarkus.dbaas.datasource.service.impl.entity.DataSourceCreationServiceImplBuilder;
import org.qubership.cloud.dbaas.client.DbaasClient;
import org.qubership.cloud.dbaas.client.entity.DbaasApiProperties;
import org.qubership.cloud.dbaas.client.entity.connection.PostgresDBConnection;
import org.qubership.cloud.dbaas.client.entity.database.DatasourceConnectorSettings;
import org.qubership.cloud.dbaas.client.entity.database.PostgresDatabase;
import org.qubership.cloud.dbaas.client.entity.database.type.PostgresDBType;
import org.qubership.cloud.dbaas.client.entity.settings.PostgresSettings;
import org.qubership.cloud.dbaas.client.management.DatabaseConfig;
import org.qubership.cloud.dbaas.client.management.DbaasDbClassifier;
import org.qubership.cloud.dbaas.client.service.LogicalDbProvider;
import org.qubership.cloud.dbaas.client.service.PostgresqlLogicalDbProvider;
import org.qubership.cloud.dbaas.common.config.DbaasApiPropertiesConfig;
import io.agroal.api.AgroalDataSource;
import io.agroal.api.configuration.AgroalConnectionFactoryConfiguration;
import io.agroal.api.configuration.AgroalConnectionPoolConfiguration;
import io.agroal.api.configuration.supplier.AgroalConnectionFactoryConfigurationSupplier;
import io.agroal.api.configuration.supplier.AgroalConnectionPoolConfigurationSupplier;
import io.agroal.api.configuration.supplier.AgroalDataSourceConfigurationSupplier;
import io.agroal.api.security.AgroalSecurityProvider;
import io.agroal.api.security.SimplePassword;
import jakarta.inject.Provider;
import javax.sql.DataSource;

import jakarta.transaction.TransactionManager;
import jakarta.transaction.TransactionSynchronizationRegistry;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.flywaydb.core.api.FlywayException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import static org.qubership.cloud.core.quarkus.dbaas.datasource.CommonTestUtils.TEST_NAMESPACE;
import static org.qubership.cloud.core.quarkus.dbaas.datasource.CommonTestUtils.getServiceClassifier;
import static org.qubership.cloud.dbaas.client.DbaasConst.SCOPE;
import static org.qubership.cloud.dbaas.client.DbaasConst.SERVICE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@Slf4j
public class DbaaSPostgresDbCreationServiceImplTest {

    private DbaasClient dbaaSClient;
    private MigrationService migrationService;
    DataSourceCreationServiceImplBuilder dataSourceCreationServiceImplBuilder;
    private final AgroalConnectionPoolConfiguration connectionPoolConfiguration = new AgroalConnectionPoolConfigurationSupplier()
            .maxSize(10).get();
    private TransactionManager transactionManager;
    private TransactionSynchronizationRegistry transactionSynchronizationRegistry;

    @BeforeEach
    public void before() {
        String logicaldbName =null;
        transactionManager = mock(TransactionManager.class);
        transactionSynchronizationRegistry = mock(TransactionSynchronizationRegistry.class);
        dbaaSClient = mock(DbaasClient.class);
        migrationService = mock(MigrationService.class);
        CoreFlywayConfig coreFlywayConfig = new CoreFlywayConfig();
        FlywayConfig flywayConfig = new FlywayConfig();
        flywayConfig.cleanAndMigrateAtStart = false;
        flywayConfig.ignoreMigrationPatterns = Optional.of(new String[]{"*:future", "*:missing"});
        coreFlywayConfig.globalFlywayConfig = flywayConfig;

        DbaasDatasourcePoolConfiguration poolConfiguration = mock(DbaasDatasourcePoolConfiguration.class);
        DatasourceProperties properties = new DatasourceProperties();
        properties.debugDatasourceListeners = false;
        properties.globalJdbcProperties = new HashMap<>();
        when(poolConfiguration.getDatasourceProperties()).thenReturn(properties);
        when(poolConfiguration.getJdbcProperties(any())).thenCallRealMethod();

        AgroalConnectionPoolConfigurationFactory agroalConnectionPoolConfigurationFactory = mock(AgroalConnectionPoolConfigurationFactory.class);
        doReturn(connectionPoolConfiguration).when(agroalConnectionPoolConfigurationFactory).createAgroalConnectionPoolConfiguration(logicaldbName, new HashMap<>());

        dataSourceCreationServiceImplBuilder =
                DataSourceCreationServiceImplBuilder.builder()
                        .setConnectionPoolConfigurationFactory(agroalConnectionPoolConfigurationFactory)
                        .setConnectionFactoryConfiguration(connectionPoolConfiguration.connectionFactoryConfiguration())
                        .setCoreFlywayConfig(coreFlywayConfig)
                        .setDbaasPoolConfiguration(poolConfiguration)
                        .setDbProviders(Collections.singletonList(new DbaaSPgLogicalDbProvider(dbaaSClient)))
                        .setMigrationService(migrationService)
                        .setNamespace(TEST_NAMESPACE)
                        .setPostgresDbConfiguration(createDefaultDbaaSPostgresDbConfiguration());

    }

    @NotNull
    private DbaaSPostgresDbCreationConfig createDefaultDbaaSPostgresDbConfiguration() {
        DbaaSPostgresDbCreationConfig dbaaSPostgresDbCreationConfig = new DbaaSPostgresDbCreationConfig();
        PostgresDbConfiguration postgresDbConfiguration = new PostgresDbConfiguration();
        postgresDbConfiguration.databaseSettings = Optional.empty();
        postgresDbConfiguration.physicalDatabaseId = Optional.empty();
        dbaaSPostgresDbCreationConfig.serviceDbConfiguration = postgresDbConfiguration;
        dbaaSPostgresDbCreationConfig.tenantDbConfiguration = Collections.singletonMap("tenant", postgresDbConfiguration);
        DbaasApiPropertiesConfig dbaasApiPropertiesConfig = mock(DbaasApiPropertiesConfig.class);
        DbaasApiProperties dbaasApiProperties = new DbaasApiProperties();
        dbaasApiProperties.setRuntimeUserRole(null);
        when(dbaasApiPropertiesConfig.getDbaaseApiProperties()).thenReturn(dbaasApiProperties);
        dbaaSPostgresDbCreationConfig.dbaasApiPropertiesConfig = dbaasApiPropertiesConfig;
        return dbaaSPostgresDbCreationConfig;
    }

    @Test
    public void mustReturnSameDataSource() {
        PostgresDatabase postgresDatabase = getPostgresDatabase("test-url", "test-username", "test-password");
        when(dbaaSClient.getOrCreateDatabase(any(), anyString(), anyMap(), any(DatabaseConfig.class))).thenReturn(postgresDatabase);


        DbaasDbClassifier classifier = getTenantClassifier("test-tenant");

        DbaaSPostgresDbCreationServiceImpl dbaaSPostgresDbCreationService = dataSourceCreationServiceImplBuilder.build();
        PostgresDatabase firstDb = dbaaSPostgresDbCreationService.getOrCreatePostgresDatabase(classifier);
        assertNotNull(firstDb);

        DataSource dataSource = firstDb.getConnectionProperties().getDataSource();
        Assumptions.assumeTrue(() -> dataSource instanceof AgroalDataSource, "dataSource must be AgroalDataSource");

        AgroalDataSource agroalDataSource = (AgroalDataSource) dataSource;
        AgroalConnectionFactoryConfiguration configuration = agroalDataSource.getConfiguration().connectionPoolConfiguration().connectionFactoryConfiguration();
        assertEquals("test-url", configuration.jdbcUrl());
        assertEquals("test-username", configuration.principal().getName());
        assertFalse(configuration.credentials().isEmpty());

        when(dbaaSClient.getConnection(any(), any(), any(), anyMap())).thenReturn(postgresDatabase.getConnectionProperties());

        PostgresDatabase secondDb = dbaaSPostgresDbCreationService.getOrCreatePostgresDatabase(classifier);
        assertEquals(firstDb, secondDb);
    }

    @Test
    public void mustUseDefaultPropertiesWhenCustomParamsEmpty() {
        DatasourceProperties properties = new DatasourceProperties();
        properties.enhancedLeakReport = true;
        JDBCConfig jdbcConfig = getJdbcConfigProps();
        properties.jdbc = jdbcConfig;

        AgroalConnectionPoolConfigurationFactory agroalConnectionPoolConfigurationFactory = new AgroalConnectionPoolConfigurationFactory(properties, transactionManager, transactionSynchronizationRegistry);
        DatasourceConnectorSettings connectorSettings = new DatasourceConnectorSettings();

        DbaaSPostgresDbCreationServiceImpl dbaaSPostgresDbCreationService = dataSourceCreationServiceImplBuilder
                .setConnectionPoolConfigurationFactory(agroalConnectionPoolConfigurationFactory)
                .build();
        DbaasDbClassifier classifier = getTenantClassifierWithLogicalDb("test-tenant");
        PostgresDatabase postgresDatabase = getPostgresDatabase("test-url", "test-username", "test-password");
        postgresDatabase.setClassifier(new TreeMap<>(classifier.asMap()));

        doReturn(postgresDatabase).when(dbaaSClient).getOrCreateDatabase(any(), anyString(), anyMap(), any(DatabaseConfig.class));

        PostgresDatabase firstdb = dbaaSPostgresDbCreationService.getOrCreatePostgresDatabase(classifier, connectorSettings, null);
        AgroalDataSource dataSource = (AgroalDataSource) firstdb.getConnectionProperties().getDataSource();
        AgroalConnectionPoolConfiguration configuration = dataSource.getConfiguration().connectionPoolConfiguration();

        assertEquals(15, configuration.maxSize());
        assertEquals(1, configuration.minSize());
        assertEquals(true, configuration.flushOnClose());
    }

    private JDBCConfig getJdbcConfigProps() {
        JDBCConfig jdbcConfig = new JDBCConfig();
        jdbcConfig.flushOnClose = true;
        jdbcConfig.datasourceRespondTimeToDrop = "10";
        jdbcConfig.poolSize = 15;
        jdbcConfig.minPoolSize = 1;
        jdbcConfig.initPoolSize = 6;
        return jdbcConfig;
    }

    @Test
    public void mustUseSpecificPropertiesForLogicalDb() {
        DatasourceProperties properties = createPropertiesData();

        Map<String, Object> jdbcPropCustomparams = new HashMap<>();
        jdbcPropCustomparams.put("Options", "-c idle-in-transaction-session-timeout=28800000");

        AgroalConnectionPoolConfigurationFactory agroalConnectionPoolConfigurationFactory = new AgroalConnectionPoolConfigurationFactory(properties, transactionManager, transactionSynchronizationRegistry);


        DatasourceConnectorSettings connectorSettings = new DatasourceConnectorSettings();
        connectorSettings.setConnPropertiesParam(jdbcPropCustomparams);
        DbaaSPostgresDbCreationServiceImpl dbaaSPostgresDbCreationService = dataSourceCreationServiceImplBuilder
                .setConnectionPoolConfigurationFactory(agroalConnectionPoolConfigurationFactory)
                .build();
        // Mock the pool configuration and factory to use the specific properties
        DbaasDatasourcePoolConfiguration poolConfiguration = mock(DbaasDatasourcePoolConfiguration.class);
        when(poolConfiguration.getDatasourceProperties()).thenReturn(properties);



        DbaasDbClassifier classifier = getTenantClassifierWithLogicalDb("test-tenant");
        PostgresDatabase postgresDatabase = getPostgresDatabase("test-url", "test-username", "test-password");
        postgresDatabase.setClassifier(new TreeMap<>(classifier.asMap()));

        when(dbaaSClient.getOrCreateDatabase(any(), anyString(), anyMap(), any(DatabaseConfig.class)))
                .thenReturn(postgresDatabase);
        PostgresDatabase firstdb = dbaaSPostgresDbCreationService.getOrCreatePostgresDatabase(classifier,connectorSettings,null);

        // Verify the connection pool configuration uses the specific properties
        AgroalDataSource dataSource = (AgroalDataSource) firstdb.getConnectionProperties().getDataSource();
        AgroalConnectionPoolConfiguration configuration = dataSource.getConfiguration().connectionPoolConfiguration();

        assertEquals(20, configuration.maxSize());
        assertEquals(5, configuration.minSize());
    }

    @Test
    public void mustUseSpecificPropertiesForCustomParams() {
        DatasourceProperties properties = createPropertiesData();
        AgroalConnectionPoolConfigurationFactory agroalConnectionPoolConfigurationFactory = new AgroalConnectionPoolConfigurationFactory(properties, transactionManager, transactionSynchronizationRegistry);

        Map<String, Object> customparams = new HashMap<>();
        customparams.put("jdbc.max-size", 3);
        customparams.put("jdbc.min-size", 1);
        customparams.put("jdbc.initial-size", 1);

        DatasourceConnectorSettings connectorSettings = new DatasourceConnectorSettings();
        connectorSettings.setConnPropertiesParam(customparams);
        DbaaSPostgresDbCreationServiceImpl dbaaSPostgresDbCreationService = dataSourceCreationServiceImplBuilder
                .setConnectionPoolConfigurationFactory(agroalConnectionPoolConfigurationFactory)
                .build();
        DbaasDbClassifier classifier = getTenantClassifierWithLogicalDb("test-tenant");
        PostgresDatabase postgresDatabase = getPostgresDatabase("test-url", "test-username", "test-password");
        postgresDatabase.setClassifier(new TreeMap<>(classifier.asMap()));

        doReturn(postgresDatabase).when(dbaaSClient).getOrCreateDatabase(any(), anyString(), anyMap(), any(DatabaseConfig.class));

        PostgresDatabase firstdb = dbaaSPostgresDbCreationService.getOrCreatePostgresDatabase(classifier, connectorSettings, null);
        AgroalDataSource dataSource = (AgroalDataSource) firstdb.getConnectionProperties().getDataSource();
        AgroalConnectionPoolConfiguration configuration = dataSource.getConfiguration().connectionPoolConfiguration();

        assertEquals(3, configuration.maxSize());
        assertEquals(1, configuration.minSize());
    }

    private DatasourceProperties createPropertiesData() {
        // Setup specific JDBC properties for a logical DB
        DatasourceProperties properties = new DatasourceProperties();
        properties.enhancedLeakReport = true;
        properties.jdbc = new JDBCConfig();
        String logicaldbName = "configs";
        DatasourceProperties.JDBCProperties specificJdbcProperties = new DatasourceProperties.JDBCProperties();
        JDBCConfig specificJdbcConfig = Mockito.spy(new JDBCConfig());
        specificJdbcConfig.poolSize = 20;
        specificJdbcConfig.minPoolSize = 5;
        specificJdbcConfig.initPoolSize=10;
        specificJdbcConfig.datasourceIdleValidationTimeout =0.10;
        specificJdbcConfig.datasourceReapTimeout =0.10;
        specificJdbcConfig.datasourceAcquisitionTimeout = 0.10;
        specificJdbcConfig.datasourceLeakDetectionInterval = 1;
        specificJdbcConfig.datasourceRespondTimeToDrop = "5";
        specificJdbcConfig.datasourceValidationInterval =7;
        specificJdbcConfig.flushOnClose = true;
        specificJdbcConfig.autoCommit = false;
        specificJdbcProperties.jdbc = specificJdbcConfig;
        properties.datasources.put(logicaldbName, specificJdbcProperties);
        return properties;
    }

    @Test
    public void testPostgresTlsWithPredefineParams() {
        PostgresDatabase postgresDatabase = getPostgresDatabase("jdbc:postgresql://localhost/test?loggerLevel=OFF", "test-username", "test-password");
        postgresDatabase.getConnectionProperties().setTls(true);
        when(dbaaSClient.getOrCreateDatabase(any(), anyString(), anyMap(), any(DatabaseConfig.class))).thenReturn(postgresDatabase);

        DbaasDbClassifier classifier = getTenantClassifier("test-tenant");

        DbaaSPostgresDbCreationServiceImpl dbaaSPostgresDbCreationService = dataSourceCreationServiceImplBuilder.build();
        PostgresDatabase firstDb = dbaaSPostgresDbCreationService.getOrCreatePostgresDatabase(classifier);
        assertNotNull(firstDb);

        DataSource dataSource = firstDb.getConnectionProperties().getDataSource();
        Assumptions.assumeTrue(() -> dataSource instanceof AgroalDataSource, "dataSource must be AgroalDataSource");

        AgroalDataSource agroalDataSource = (AgroalDataSource) dataSource;
        AgroalConnectionFactoryConfiguration configuration = agroalDataSource.getConfiguration().connectionPoolConfiguration().connectionFactoryConfiguration();
        assertEquals("jdbc:postgresql://localhost/test?loggerLevel=OFF&sslmode=verify-full&sslfactory=org.postgresql.ssl.DefaultJavaSSLFactory", configuration.jdbcUrl());
        assertEquals("test-username", configuration.principal().getName());
        assertFalse(configuration.credentials().isEmpty());

        when(dbaaSClient.getConnection(any(), any(), any(), anyMap())).thenReturn(postgresDatabase.getConnectionProperties());

        PostgresDatabase secondDb = dbaaSPostgresDbCreationService.getOrCreatePostgresDatabase(classifier);
        assertEquals(firstDb, secondDb);
    }

    @Test
    public void testPostgresTlsNotStrictWithPredefineParams() {
        PostgresDatabase postgresDatabase = getPostgresDatabase("jdbc:postgresql://localhost/test?loggerLevel=OFF", "test-username", "test-password");
        postgresDatabase.getConnectionProperties().setTls(true);
        postgresDatabase.getConnectionProperties().setTlsNotStrict(true);
        when(dbaaSClient.getOrCreateDatabase(any(), anyString(), anyMap(), any(DatabaseConfig.class))).thenReturn(postgresDatabase);

        DbaasDbClassifier classifier = getTenantClassifier("test-tenant");

        DbaaSPostgresDbCreationServiceImpl dbaaSPostgresDbCreationService = dataSourceCreationServiceImplBuilder.build();
        PostgresDatabase firstDb = dbaaSPostgresDbCreationService.getOrCreatePostgresDatabase(classifier);
        assertNotNull(firstDb);

        DataSource dataSource = firstDb.getConnectionProperties().getDataSource();
        Assumptions.assumeTrue(() -> dataSource instanceof AgroalDataSource, "dataSource must be AgroalDataSource");

        AgroalDataSource agroalDataSource = (AgroalDataSource) dataSource;
        AgroalConnectionFactoryConfiguration configuration = agroalDataSource.getConfiguration().connectionPoolConfiguration().connectionFactoryConfiguration();
        assertEquals("jdbc:postgresql://localhost/test?loggerLevel=OFF&sslmode=require&sslfactory=org.postgresql.ssl.DefaultJavaSSLFactory", configuration.jdbcUrl());
        assertEquals("test-username", configuration.principal().getName());
        assertFalse(configuration.credentials().isEmpty());

        when(dbaaSClient.getConnection(any(), any(), any(), anyMap())).thenReturn(postgresDatabase.getConnectionProperties());

        PostgresDatabase secondDb = dbaaSPostgresDbCreationService.getOrCreatePostgresDatabase(classifier);
        assertEquals(firstDb, secondDb);
    }

    @Test
    public void mustCreateDifferentDatabasesForDifferentTenants() throws SQLException {
        PostgresDatabase postgresDatabase = getPostgresDatabase("test-url", "test-username", "test-password");
        DbaasDbClassifier classifier = getTenantClassifier("test-tenant");
        when(dbaaSClient.getOrCreateDatabase(any(), anyString(), eq(classifier.asMap()), any(DatabaseConfig.class))).thenReturn(postgresDatabase);

        DbaaSPostgresDbCreationServiceImpl dbaaSPostgresDbCreationService = dataSourceCreationServiceImplBuilder.build();
        PostgresDatabase firstDb = dbaaSPostgresDbCreationService.getOrCreatePostgresDatabase(classifier);
        assertNotNull(firstDb);

        PostgresDatabase anotherPostgresDatabase = getPostgresDatabase("test-url-2", "test-username-2", "test-password-2");

        Map<String, Object> anotherParams = new HashMap<>();
        anotherParams.put("microserviceName", "test-service");
        anotherParams.put("tenantId", "another-test-tenant");
        DbaasDbClassifier anotherClassifier = new DbaasDbClassifier(anotherParams);
        when(dbaaSClient.getOrCreateDatabase(any(), anyString(), eq(anotherParams), any(DatabaseConfig.class))).thenReturn(anotherPostgresDatabase);
        PostgresDatabase secondDb = dbaaSPostgresDbCreationService.getOrCreatePostgresDatabase(anotherClassifier);
        assertNotEquals(firstDb, secondDb);
        verify(migrationService, times(2)).migrate(any(), isNull());
    }

    @Test
    public void testCanCreateServiceDatabaseWithPgExtensions() {
        PostgresDbConfiguration dbConfiguration = getPostgresDbConfiguration();
        DbaaSPostgresDbCreationConfig postgresDbConfiguration = new DbaaSPostgresDbCreationConfig();
        DbaasApiPropertiesConfig dbaasApiPropertiesConfig = mock(DbaasApiPropertiesConfig.class);
        DbaasApiProperties dbaasApiProperties = new DbaasApiProperties();
        dbaasApiProperties.setRuntimeUserRole(null);
        when(dbaasApiPropertiesConfig.getDbaaseApiProperties()).thenReturn(dbaasApiProperties);
        postgresDbConfiguration.dbaasApiPropertiesConfig = dbaasApiPropertiesConfig;
        postgresDbConfiguration.serviceDbConfiguration = dbConfiguration;
        dataSourceCreationServiceImplBuilder.setPostgresDbConfiguration(postgresDbConfiguration);

        DbaaSPostgresDbCreationServiceImpl dbaaSPostgresDbCreationService = dataSourceCreationServiceImplBuilder.build();
        DbaasDbClassifier classifier = getServiceClassifier();

        PostgresDatabase postgresDatabaseWithExtensions = getPostgresDatabase("test-url", "test-username", "test-password");
        when(dbaaSClient.getOrCreateDatabase(any(), anyString(), anyMap(), any(DatabaseConfig.class))).thenReturn(postgresDatabaseWithExtensions);
        dbaaSPostgresDbCreationService.getOrCreatePostgresDatabase(classifier);
        verify(dbaaSClient).getOrCreateDatabase(eq(PostgresDBType.INSTANCE), anyString(), eq(classifier.asMap()),
                argThat((ArgumentMatcher<DatabaseConfig>) parameters -> parameters.getDatabaseSettings().equals(dbConfiguration.databaseSettings.get()) && parameters.getPhysicalDatabaseId().equals("123")));
    }

    private PostgresDbConfiguration getPostgresDbConfiguration() {
        PostgresDbConfiguration dbConfiguration = new PostgresDbConfiguration();
        dbConfiguration.physicalDatabaseId = Optional.of("123");
        PostgresSettings settings = new PostgresSettings();
        settings.setPgExtensions(Collections.singletonList("bloom"));
        dbConfiguration.databaseSettings = Optional.of(settings);
        return dbConfiguration;
    }

    @Test
    public void testCanCreateTenantDatabaseWithPgExtensions() {
        DbaaSPostgresDbCreationConfig tenantDbConfiguration = new DbaaSPostgresDbCreationConfig();
        DbaasApiPropertiesConfig dbaasApiPropertiesConfig = mock(DbaasApiPropertiesConfig.class);
        DbaasApiProperties dbaasApiProperties = new DbaasApiProperties();
        dbaasApiProperties.setRuntimeUserRole(null);
        when(dbaasApiPropertiesConfig.getDbaaseApiProperties()).thenReturn(dbaasApiProperties);
        tenantDbConfiguration.dbaasApiPropertiesConfig = dbaasApiPropertiesConfig;
        PostgresDbConfiguration dbConfiguration = getPostgresDbConfiguration();
        tenantDbConfiguration.allTenantsDbConfiguration = dbConfiguration;
        DbaasDbClassifier classifier = getTenantClassifier("test-tenant");

        PostgresDatabase postgresDatabaseWithExtensions = getPostgresDatabase("test-url", "test-username", "test-password");
        when(dbaaSClient.getOrCreateDatabase(any(), anyString(), anyMap(), any(DatabaseConfig.class))).thenReturn(postgresDatabaseWithExtensions);

        DbaaSPostgresDbCreationServiceImpl dbaaSPostgresDbCreationService = dataSourceCreationServiceImplBuilder
                .setPostgresDbConfiguration(tenantDbConfiguration).build();
        dbaaSPostgresDbCreationService.getOrCreatePostgresDatabase(classifier);
        verify(dbaaSClient).getOrCreateDatabase(eq(PostgresDBType.INSTANCE), anyString(), eq(classifier.asMap()),
                argThat((ArgumentMatcher<DatabaseConfig>) parameters -> parameters.getDatabaseSettings().equals(dbConfiguration.databaseSettings.get()) && parameters.getPhysicalDatabaseId().equals("123")));
    }

    private PostgresDatabase getPostgresDatabase(String url, String username, String password) {
        PostgresDatabase postgresDatabase = new PostgresDatabase();
        postgresDatabase.setConnectionProperties(getPostgresDBConnection(url, username, password));
        return postgresDatabase;
    }

    private PostgresDBConnection getPostgresDBConnection(String url, String username, String password){
        return new PostgresDBConnection(url, username, password, "admin");
    }

    @Test
    public void provideDbByCustomLogicDbProvider() {
        String url = "custom_url", username = "custom_username", password = "custom_password";
        PostgresqlLogicalDbProvider customProvider = new PostgresqlLogicalDbProvider() {
            @Override
            public @Nullable PostgresConnectionProperty provideConnectionProperty(SortedMap<String, Object> classifier, DatabaseConfig params) {
                return new PostgresConnectionProperty(url, username, password, params.getUserRole(), false);
            }

        };
        DbaaSPostgresDbCreationServiceImpl dbaaSPostgresDbCreationService = dataSourceCreationServiceImplBuilder
                .setDbProviders(Arrays.asList(new DbaaSPgLogicalDbProvider(dbaaSClient), customProvider))
                .build();

        PostgresDatabase postgresDatabase = dbaaSPostgresDbCreationService.getOrCreatePostgresDatabase(new DbaasDbClassifier(Collections.EMPTY_MAP));

        PostgresDBConnection connectionProperties = postgresDatabase.getConnectionProperties();
        assertEquals(url, connectionProperties.getUrl());
        assertEquals(username, connectionProperties.getUsername());
        assertEquals(password, connectionProperties.getPassword());
    }

    @Test
    public void getLogicalDbAfterCustomProviderReturnNull() {
        PostgresqlLogicalDbProvider customProvider = new PostgresqlLogicalDbProvider() {
            @Override
            public @Nullable PostgresConnectionProperty provideConnectionProperty(SortedMap<String, Object> classifier, DatabaseConfig params) {
                return null;
            }
        };
        String url = "pg-url", username = "username", password = "pwd";
        when(dbaaSClient.getOrCreateDatabase(any(), anyString(), anyMap(), any(DatabaseConfig.class))).thenReturn(getPostgresDatabase(url, username, password));
        DbaaSPostgresDbCreationServiceImpl dbaaSPostgresDbCreationService = dataSourceCreationServiceImplBuilder
                .setDbProviders(Arrays.asList(new DbaaSPgLogicalDbProvider(dbaaSClient), customProvider))
                .build();
        PostgresDatabase postgresDatabase = dbaaSPostgresDbCreationService.getOrCreatePostgresDatabase(new DbaasDbClassifier(Collections.EMPTY_MAP));

        PostgresDBConnection connectionProperties = postgresDatabase.getConnectionProperties();
        assertEquals(url, connectionProperties.getUrl());
        assertEquals(username, connectionProperties.getUsername());
        assertEquals(password, connectionProperties.getPassword());
    }

    @Test
    public void throwExceptionAfterAllDbProvidersReturnNull() {
        PostgresqlLogicalDbProvider customProvider = new PostgresqlLogicalDbProvider() {
            @Override
            public @Nullable PostgresConnectionProperty provideConnectionProperty(SortedMap<String, Object> classifier, DatabaseConfig params) {
                return null;
            }

        };
        when(dbaaSClient.getOrCreateDatabase(any(), anyString(), anyMap(), any(DatabaseConfig.class))).thenReturn(null);
        List<PostgresqlLogicalDbProvider> dbProviders = Arrays.asList(new DbaaSPgLogicalDbProvider(dbaaSClient), customProvider);
        DbaaSPostgresDbCreationServiceImpl dbaaSPostgresDbCreationService = dataSourceCreationServiceImplBuilder
                .setDbProviders(dbProviders)
                .build();
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> dbaaSPostgresDbCreationService.getOrCreatePostgresDatabase(new DbaasDbClassifier(Collections.EMPTY_MAP)));
        dbProviders.sort(Comparator.comparingInt(LogicalDbProvider::order));
        assertEquals("Not one of the providers: " + dbProviders + " could provide a logical postgresql database", exception.getMessage());
    }

    @Test
    public void throwExceptionAfterDbWasProvidedButConnIsNull() {
        PostgresDatabase postgresDatabase = new PostgresDatabase();
        when(dbaaSClient.getOrCreateDatabase(any(), anyString(), anyMap(), any(DatabaseConfig.class))).thenReturn(postgresDatabase);
        DbaaSPgLogicalDbProvider dbProvider = new DbaaSPgLogicalDbProvider(dbaaSClient);
        DbaaSPostgresDbCreationServiceImpl dbaaSPostgresDbCreationService = dataSourceCreationServiceImplBuilder
                .setDbProviders(Collections.singletonList(dbProvider))
                .build();

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> dbaaSPostgresDbCreationService.getOrCreatePostgresDatabase(new DbaasDbClassifier(Collections.EMPTY_MAP)));

        assertEquals("Provider: " + dbProvider + "have provided postgresql database " +
                "but connection properties is null", exception.getMessage());
    }

    @Test
    public void updatesPasswordsFromDbaasProperly() throws IllegalAccessException {
        SimplePassword firstPassword = new SimplePassword("first-password");
        SimplePassword secondPassword = new SimplePassword("second-password");

        PostgresDatabase db1 = getDbWithCredentialsInDatasource(firstPassword);
        PostgresDatabase db2 = getDbWithCredentialsInDatasource(secondPassword);

        SortedMap<String, Object> clf1 = new TreeMap<>(Map.of("first-key", "first-val"));
        SortedMap<String, Object> clf2 = new TreeMap<>(Map.of("second-key", "second-val"));

        db1.setClassifier(clf1);
        db2.setClassifier(clf2);

        Map<DatabaseKey, PostgresDatabase> postgresDatabaseMap = new ConcurrentHashMap<>();
        postgresDatabaseMap.put(new DatabaseKey(clf1, null), db1);
        postgresDatabaseMap.put(new DatabaseKey(clf2, null), db2);

        DbaaSPostgresDbCreationServiceImpl creationService = dataSourceCreationServiceImplBuilder.build();
        Provider provider = Mockito.mock(Provider.class);
        Mockito.when(provider.get()).thenReturn(null);

        FieldUtils.writeField(creationService, "postgresDbMap", postgresDatabaseMap, true);

        SimplePassword freshFirstPassword = new SimplePassword("fresh-first-password");
        SimplePassword freshSecondPassword = new SimplePassword("fresh-second-password");
        PostgresDatabase freshDb1 = getDbWithCredentialsInDatasource(freshFirstPassword);
        PostgresDatabase freshDb2 = getDbWithCredentialsInDatasource(freshSecondPassword);

        Mockito.when(dbaaSClient.getOrCreateDatabase(Mockito.eq(
                PostgresDBType.INSTANCE),
                Mockito.anyString(),
                Mockito.eq(clf1),
                Mockito.any(DatabaseConfig.class))
        ).thenReturn(freshDb1);
        when(dbaaSClient.getConnection(any(), any(), any(), Mockito.eq(clf1))).thenReturn(freshDb1.getConnectionProperties());

        Mockito.when(dbaaSClient.getOrCreateDatabase(Mockito.eq(
                PostgresDBType.INSTANCE),
                Mockito.anyString(),
                Mockito.eq(clf2),
                Mockito.any(DatabaseConfig.class))
        ).thenReturn(freshDb2);
        when(dbaaSClient.getConnection(any(), any(), any(), Mockito.eq(clf2))).thenReturn(freshDb2.getConnectionProperties());


        creationService.updatePostgresDatabasesPasswords(new DbaasDbClassifier(clf1));
        creationService.updatePostgresDatabasesPasswords(new DbaasDbClassifier(clf2));

        Properties firstPropertiesAfterUpdate = getSecurityProvider(((AgroalDataSource) db1.getConnectionProperties().getDataSource())).getSecurityProperties(firstPassword);
        Properties secondPropertiesAfterUpdate = getSecurityProvider(((AgroalDataSource) db2.getConnectionProperties().getDataSource())).getSecurityProperties(secondPassword);

        assertEquals(freshFirstPassword.getWord(), firstPropertiesAfterUpdate.getProperty("password"));
        assertEquals(freshSecondPassword.getWord(), secondPropertiesAfterUpdate.getProperty("password"));
    }

    @Test
    public void testCreateMultipleDatasources() throws Exception {
        DbaaSPostgresDbCreationServiceImpl creationService = dataSourceCreationServiceImplBuilder.build();

        String tenantUsername = "tenant-username";
        String tenantPassword = "tenant-password";
        String tenantUrl = "tenant-url";
        String serviceUsername = "service-username";
        String servicePassword = "service-password";
        String serviceUrl = "service-url";

        PostgresDatabase tenantDatabase = getPostgresDatabase(tenantUrl, tenantUsername, tenantPassword);
        PostgresDatabase serviceDatabase = getPostgresDatabase(serviceUrl, serviceUsername, servicePassword);

        DbaasDbClassifier tenantClassifier = new DbaasDbClassifier(Map.of(SCOPE, SERVICE));
        DbaasDbClassifier serviceClassifier = new DbaasDbClassifier(Map.of("tenantId", "test-tenant-id"));

        when(dbaaSClient.getOrCreateDatabase(any(), anyString(), Mockito.eq(tenantClassifier.asMap()), any(DatabaseConfig.class))).thenReturn(tenantDatabase);
        when(dbaaSClient.getOrCreateDatabase(any(), anyString(), Mockito.eq(serviceClassifier.asMap()), any(DatabaseConfig.class))).thenReturn(serviceDatabase);
        when(dbaaSClient.getConnection(any(), eq(null), eq(null), Mockito.eq(tenantClassifier.asMap()))).
                thenReturn(tenantDatabase.getConnectionProperties());
        when(dbaaSClient.getConnection(any(), eq(null), eq(null), Mockito.eq(serviceClassifier.asMap()))).
                thenReturn(serviceDatabase.getConnectionProperties());
        AgroalDataSource tenantDatasource = (AgroalDataSource) creationService.getOrCreatePostgresDatabase(tenantClassifier).getConnectionProperties().getDataSource();
        AgroalDataSource serviceDatasource = (AgroalDataSource) creationService.getOrCreatePostgresDatabase(serviceClassifier).getConnectionProperties().getDataSource();
        AgroalConnectionFactoryConfiguration tenantConnectionFactory = tenantDatasource.getConfiguration()
                .connectionPoolConfiguration().connectionFactoryConfiguration();
        AgroalConnectionFactoryConfiguration serviceConnectionFactory = serviceDatasource.getConfiguration()
                .connectionPoolConfiguration().connectionFactoryConfiguration();

        assertNotSame(tenantDatasource, serviceDatasource);

        assertEquals(tenantUsername, tenantConnectionFactory.principal().getName());
        assertEquals(1, tenantConnectionFactory.credentials().size());
        assertTrue(tenantConnectionFactory.credentials().contains(new SimplePassword(tenantPassword)));
        assertTrue(tenantConnectionFactory.jdbcUrl().contains(tenantUrl));

        assertEquals(serviceUsername, serviceConnectionFactory.principal().getName());
        assertEquals(1, serviceConnectionFactory.credentials().size());
        assertTrue(serviceConnectionFactory.credentials().contains(new SimplePassword(servicePassword)));
        assertTrue(serviceConnectionFactory.jdbcUrl().contains(serviceUrl));
    }

    @Test
    public void mustFreeResourcesOnMigrationFailure() {
        PostgresDatabase postgresDatabase = getPostgresDatabase("test-url", "test-username", "test-password");
        when(dbaaSClient.getOrCreateDatabase(any(), anyString(), anyMap(), any(DatabaseConfig.class))).thenReturn(postgresDatabase);
        when(dbaaSClient.getConnection(any(), eq(null), eq(null), anyMap())).
                thenReturn(postgresDatabase.getConnectionProperties());

        FlywayException migrationException = new FlywayException();
        when(migrationService.migrate(Mockito.any(DataSource.class), isNull())).thenThrow(migrationException);

        DbaaSPostgresDbCreationServiceImpl dbaaSPostgresDbCreationService = dataSourceCreationServiceImplBuilder.build();
        assertThrows(FlywayException.class, () -> dbaaSPostgresDbCreationService.getOrCreatePostgresDatabase(getServiceClassifier()));
    }

    private DbaasDbClassifier getTenantClassifier(String tenantId) {
        Map<String, Object> params = new HashMap<>();
        params.put("microserviceName", "test-service");
        params.put("tenantId", tenantId);
        return new DbaasDbClassifier(params);
    }

    private DbaasDbClassifier getTenantClassifierWithLogicalDb(String tenantId) {
        Map<String, Object> params = new HashMap<>();
        Map<String, Object> customparams = new HashMap<>();
        customparams.put("logicalDbName","configs");
        params.put("microserviceName", "test-service");
        params.put("tenantId", tenantId);
        params.put("customKeys",customparams);
        return new DbaasDbClassifier(params);
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

    private PostgresDatabase getDbWithCredentialsInDatasource(SimplePassword password) {
        PostgresDatabase db = new PostgresDatabase();

        AgroalDataSource datasource = Mockito.mock(AgroalDataSource.class);
        AgroalDataSourceConfigurationSupplier dsConfigSupplier = new AgroalDataSourceConfigurationSupplier();
        AgroalConnectionPoolConfigurationSupplier connPoolConfigSupplier = new AgroalConnectionPoolConfigurationSupplier(connectionPoolConfiguration);

        AgroalConnectionFactoryConfiguration connectionFactoryConfiguration = new AgroalConnectionFactoryConfigurationBuilder()
                .credential(password)
                .securityProvider(new DbaasSecurityProvider(password))
                .build();
        AgroalConnectionFactoryConfigurationSupplier connFactorySupplier = new AgroalConnectionFactoryConfigurationSupplier(connectionFactoryConfiguration);
        connPoolConfigSupplier.connectionFactoryConfiguration(connFactorySupplier);
        dsConfigSupplier.connectionPoolConfiguration(connPoolConfigSupplier);
        Mockito.when(datasource.getConfiguration()).thenReturn(dsConfigSupplier.get());

        PostgresDBConnection dbConn = new PostgresDBConnection();
        dbConn.setPassword(password.getWord());
        dbConn.setDataSource(datasource);
        db.setConnectionProperties(dbConn);

        return db;
    }
}

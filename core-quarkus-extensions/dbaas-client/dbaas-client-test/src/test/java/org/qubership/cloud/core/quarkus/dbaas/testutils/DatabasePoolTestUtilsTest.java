package org.qubership.cloud.core.quarkus.dbaas.testutils;


import org.qubership.cloud.core.quarkus.dbaas.datasource.DbaaSDataSource;
import org.qubership.cloud.core.quarkus.dbaas.datasource.classifier.MicroserviceClassifierBuilder;
import org.qubership.cloud.core.quarkus.dbaas.datasource.config.AgroalConnectionPoolConfigurationFactory;
import org.qubership.cloud.core.quarkus.dbaas.datasource.config.DbaasDatasourcePoolConfiguration;
import org.qubership.cloud.core.quarkus.dbaas.datasource.config.flyway.CoreFlywayConfig;
import org.qubership.cloud.core.quarkus.dbaas.datasource.config.flyway.FlywayConfig;
import org.qubership.cloud.core.quarkus.dbaas.datasource.config.properties.DatasourceProperties;
import org.qubership.cloud.core.quarkus.dbaas.datasource.config.properties.DbaaSPostgresDbCreationConfig;
import org.qubership.cloud.core.quarkus.dbaas.datasource.service.MigrationService;
import org.qubership.cloud.core.quarkus.dbaas.datasource.service.impl.DbaaSPostgresDbCreationServiceImpl;
import org.qubership.cloud.dbaas.client.entity.connection.PostgresDBConnection;
import org.qubership.cloud.dbaas.client.entity.database.PostgresDatabase;
import org.qubership.cloud.dbaas.client.management.DatabaseConfig;
import org.qubership.cloud.dbaas.client.management.DbaasDbClassifier;
import org.qubership.cloud.dbaas.client.service.PostgresqlLogicalDbProvider;
import org.qubership.cloud.dbaas.common.config.DbaasApiPropertiesConfig;
import io.agroal.api.AgroalDataSource;
import io.agroal.api.configuration.AgroalConnectionFactoryConfiguration;
import io.agroal.api.configuration.AgroalConnectionPoolConfiguration;
import io.agroal.api.configuration.supplier.AgroalDataSourceConfigurationSupplier;
import jakarta.enterprise.inject.Instance;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

public class DatabasePoolTestUtilsTest {
    private static final String TEST_MS_NAME = "test-ms";
    private static final String TEST_NAMESPACE = "test-namespace";

    @Test
    void clearDatabasePoolCacheTest() {
        String logicalDbname = null;
        Instance<PostgresqlLogicalDbProvider> providerOfLogicalDb = mock(Instance.class);
        PostgresqlLogicalDbProvider logicalDbProvider = mock(PostgresqlLogicalDbProvider.class);
        when(logicalDbProvider.order()).thenReturn(100);
        when(providerOfLogicalDb.stream()).thenReturn(Stream.of(logicalDbProvider));

        DbaasApiPropertiesConfig apiConfig = new DbaasApiPropertiesConfig();
        apiConfig.runtimeUserRole = Optional.of("admin");
        apiConfig.dbPrefix = Optional.empty();
        DbaaSPostgresDbCreationConfig config = mock(DbaaSPostgresDbCreationConfig.class);
        config.dbaasApiPropertiesConfig = apiConfig;

        AgroalConnectionPoolConfiguration agroalPoolConfiguration = mock(AgroalConnectionPoolConfiguration.class);
        when(agroalPoolConfiguration.maxSize()).thenReturn(5);
        when(agroalPoolConfiguration.minSize()).thenReturn(1);

        DatasourceProperties properties = new DatasourceProperties();
        properties.globalJdbcProperties = new HashMap<>();
        properties.debugDatasourceListeners = false;
        DbaasDatasourcePoolConfiguration poolConfiguration = mock(DbaasDatasourcePoolConfiguration.class);
        when(poolConfiguration.getDatasourceProperties()).thenReturn(properties);
        when(poolConfiguration.getJdbcProperties(any())).thenCallRealMethod();

        CoreFlywayConfig flywayConfig = new CoreFlywayConfig();
        flywayConfig.globalFlywayConfig = new FlywayConfig();
        flywayConfig.globalFlywayConfig.cleanAndMigrateAtStart = false;

        AgroalConnectionPoolConfigurationFactory poolConfigFactory = mock(AgroalConnectionPoolConfigurationFactory.class);
        when(poolConfigFactory.createAgroalConnectionPoolConfiguration(logicalDbname, new HashMap<String,Object>())).thenReturn(agroalPoolConfiguration);
        DbaaSPostgresDbCreationServiceImpl creationService = new DbaaSPostgresDbCreationServiceImpl(
                TEST_NAMESPACE,
                mock(MigrationService.class),
                flywayConfig,
                poolConfigFactory,
                mock(AgroalConnectionFactoryConfiguration.class),
                config,
                poolConfiguration,
                providerOfLogicalDb,
                null
        );

        PostgresDBConnection connection = new PostgresDBConnection();
        connection.setUrl("some-url");
        connection.setUsername("some-username");
        connection.setPassword("some-password");
        PostgresDatabase db = mock(PostgresDatabase.class);
        when(db.getConnectionProperties()).thenReturn(connection);
        when(logicalDbProvider.provide(any(), any(), anyString())).thenReturn(db);

        try (MockedStatic<AgroalDataSource> utilities = Mockito.mockStatic(AgroalDataSource.class)) {
            utilities.when(() -> AgroalDataSource.from(any(AgroalDataSourceConfigurationSupplier.class)))
                    .thenReturn(new DbaaSDataSource(null, null));

            DbaasDbClassifier dbClassifier = new MicroserviceClassifierBuilder(new HashMap<>()).build();
            creationService.getOrCreatePostgresDatabase(dbClassifier);
            creationService.getOrCreatePostgresDatabase(dbClassifier);  // return from cache

            SortedMap<String, Object> sortedClassifier = new TreeMap<>();
            sortedClassifier.putAll(dbClassifier.asMap());
            Mockito.verify(logicalDbProvider, times(1)).provide(
                    eq(sortedClassifier),
                    any(DatabaseConfig.class),
                    eq(TEST_NAMESPACE));

            DatabasePoolTestUtils databasePoolTestUtil = new DatabasePoolTestUtils(creationService);
            databasePoolTestUtil.clearCache();

            creationService.getOrCreatePostgresDatabase(dbClassifier);

            Mockito.verify(logicalDbProvider, times(2)).provide(
                    eq(sortedClassifier),
                    any(DatabaseConfig.class),
                    eq(TEST_NAMESPACE));
        }
    }
}
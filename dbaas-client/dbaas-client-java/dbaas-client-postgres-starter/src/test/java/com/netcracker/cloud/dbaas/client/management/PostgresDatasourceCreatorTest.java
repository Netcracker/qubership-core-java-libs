package org.qubership.cloud.dbaas.client.management;

import org.qubership.cloud.dbaas.client.config.DbaasPostgresDataSourceProperties;
import org.qubership.cloud.dbaas.client.entity.connection.PostgresDBConnection;
import org.qubership.cloud.dbaas.client.entity.database.DatasourceConnectorSettings;
import org.qubership.cloud.dbaas.client.entity.database.PostgresDatabase;
import org.qubership.cloud.dbaas.client.entity.database.type.PostgresDBType;
import org.qubership.cloud.dbaas.client.metrics.DbaaSMetricsRegistrar;
import org.qubership.cloud.dbaas.client.testconfiguration.TestPostgresConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import javax.sql.DataSource;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

import static org.qubership.cloud.dbaas.client.DbaasConst.*;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTest
@ContextConfiguration(classes = {TestPostgresConfig.class})
@TestPropertySource(properties = {
        "dbaas.postgres.datasource.maximumPoolSize=10",
        "dbaas.postgres.datasource.minimumIdle=2",
        "dbaas.postgres.datasource.connectionTimeout=10000",
        "dbaas.postgres.datasource.idleTimeout=10000",
        "dbaas.postgres.datasource.maxLifetime=50000",
        "dbaas.postgres.datasource.leakDetectionThreshold=10001",
        "dbaas.postgres.datasource.autoCommit=false",
        "dbaas.postgres.datasource.initializationFailTimeout=-1",
        "dbaas.postgres.datasource.connection-properties=reWriteBatchedInserts=true"
})
public class PostgresDatasourceCreatorTest {
    public final static String DB_NAME = "dbName";
    public final static String DB_USER = "dbaas";
    public final static String DB_PASSWORD = "dbaas";
    public final static String DB_SCHEMA = "test_schema";
    public final static String POSTGRES_TEST_URI = "jdbc:postgresql://localhost/";

    @Autowired
    private PostgresDatasourceCreator postgresDatasourceCreator;

    @Autowired
    private DbaasPostgresDataSourceProperties dbaasDsProperties;

    @Test
    public void testPostgresOptionsAreApplied() throws NoSuchFieldException, IllegalAccessException {
        PostgresDatabase postgresDatabase = new PostgresDatabase();
        postgresDatabase.setName(DB_NAME);

        PostgresDBConnection postgresDBConnection = new PostgresDBConnection(POSTGRES_TEST_URI + DB_NAME, DB_USER, DB_PASSWORD, ADMIN_ROLE);
        postgresDBConnection.setTls(true);
        postgresDatabase.setConnectionProperties(postgresDBConnection);

        DatasourceConnectorSettings settings = new DatasourceConnectorSettings();
        settings.setConnPropertiesParam(Map.of("initializationFailTimeout", -1));
        postgresDatasourceCreator.create(postgresDatabase, settings);

        final DataSource dataSource = postgresDatabase.getConnectionProperties().getDataSource();
        assertNotNull(dataSource);
        assertTrue(dataSource instanceof HikariDataSource);
        final HikariDataSource hikariDataSource = (HikariDataSource) dataSource;
        Properties dataSourceProperties = hikariDataSource.getDataSourceProperties();
        assertEquals("-c idle-in-transaction-session-timeout=28800000", dataSourceProperties.get("options"));
        assertEquals("true", dataSourceProperties.get("reWriteBatchedInserts"));
        assertEquals("jdbc:postgresql://localhost/dbName?sslmode=verify-full&sslfactory=org.postgresql.ssl.DefaultJavaSSLFactory", ((HikariDataSource) dataSource).getJdbcUrl());
        assertEquals(10, hikariDataSource.getMaximumPoolSize());
        assertEquals(2, hikariDataSource.getMinimumIdle());
        assertEquals(10000, hikariDataSource.getConnectionTimeout());
        assertEquals(10000, hikariDataSource.getIdleTimeout());
        assertEquals(50000, hikariDataSource.getMaxLifetime());
        assertEquals(10001, hikariDataSource.getLeakDetectionThreshold());
        assertEquals(false, hikariDataSource.isAutoCommit());
    }

    @Test
    public void testPostgresTlsWithPredefineParams() throws NoSuchFieldException, IllegalAccessException {
        PostgresDatabase postgresDatabase = new PostgresDatabase();
        postgresDatabase.setName(DB_NAME);

        PostgresDBConnection postgresDBConnection = new PostgresDBConnection(POSTGRES_TEST_URI + DB_NAME + "?loggerLevel=OFF", DB_USER, DB_PASSWORD, ADMIN_ROLE);
        postgresDBConnection.setTls(true);
        postgresDatabase.setConnectionProperties(postgresDBConnection);

        DatasourceConnectorSettings settings = new DatasourceConnectorSettings();
        settings.setConnPropertiesParam(Map.of("initializationFailTimeout", -1));
        postgresDatasourceCreator.create(postgresDatabase, settings);

        final DataSource dataSource = postgresDatabase.getConnectionProperties().getDataSource();
        assertNotNull(dataSource);
        assertTrue(dataSource instanceof HikariDataSource);
        final HikariDataSource hikariDataSource = (HikariDataSource) dataSource;
        Properties dataSourceProperties = hikariDataSource.getDataSourceProperties();

        assertEquals("jdbc:postgresql://localhost/dbName?loggerLevel=OFF&sslmode=verify-full&sslfactory=org.postgresql.ssl.DefaultJavaSSLFactory", ((HikariDataSource) dataSource).getJdbcUrl());
    }

    @Test
    void testPostgresTlsNotStrictWithPredefineParams(){
        PostgresDatabase postgresDatabase = new PostgresDatabase();
        postgresDatabase.setName(DB_NAME);

        PostgresDBConnection postgresDBConnection = new PostgresDBConnection(POSTGRES_TEST_URI + DB_NAME + "?loggerLevel=OFF", DB_USER, DB_PASSWORD, ADMIN_ROLE);
        postgresDBConnection.setTls(true);
        postgresDBConnection.setTlsNotStrict(true);
        postgresDatabase.setConnectionProperties(postgresDBConnection);

        DatasourceConnectorSettings settings = new DatasourceConnectorSettings();
        settings.setConnPropertiesParam(Map.of("initializationFailTimeout", -1));
        postgresDatasourceCreator.create(postgresDatabase, settings);

        final DataSource dataSource = postgresDatabase.getConnectionProperties().getDataSource();
        assertNotNull(dataSource);
        assertTrue(dataSource instanceof HikariDataSource);
        final HikariDataSource hikariDataSource = (HikariDataSource) dataSource;
        Properties dataSourceProperties = hikariDataSource.getDataSourceProperties();
        assertEquals("jdbc:postgresql://localhost/dbName?loggerLevel=OFF&sslmode=require&sslfactory=org.postgresql.ssl.DefaultJavaSSLFactory", ((HikariDataSource) dataSource).getJdbcUrl());
    }

    @Test
    public void testDatasourceWithRoReplica(){
        PostgresDatabase postgresDatabase = new PostgresDatabase();
        postgresDatabase.setName(DB_NAME);
        String roHost = "test-ro-host";
        PostgresDBConnection postgresDBConnection = new PostgresDBConnection(POSTGRES_TEST_URI + DB_NAME, DB_USER, DB_PASSWORD, ADMIN_ROLE);
        postgresDBConnection.setRoHost(roHost);
        postgresDBConnection.setHost("localhost");
        postgresDatabase.setConnectionProperties(postgresDBConnection);

        DatasourceConnectorSettings settings = new DatasourceConnectorSettings();
        settings.setRoReplica(true);
        postgresDatasourceCreator.create(postgresDatabase, settings);

        final DataSource dataSource = postgresDatabase.getConnectionProperties().getDataSource();
        assertNotNull(dataSource);
        assertTrue(dataSource instanceof HikariDataSource);
        final HikariDataSource hikariDataSource = (HikariDataSource) dataSource;
        String url = hikariDataSource.getJdbcUrl();
        assertNotNull(url);
        assertTrue(url.contains(roHost));
    }

    @Test
    public void testDatasourceWithRoReplicaWithRoHostNotDefined(){
        PostgresDatabase postgresDatabase = new PostgresDatabase();
        postgresDatabase.setName(DB_NAME);
        PostgresDBConnection postgresDBConnection = new PostgresDBConnection(POSTGRES_TEST_URI + DB_NAME, DB_USER, DB_PASSWORD, ADMIN_ROLE);
        postgresDBConnection.setHost("localhost");
        postgresDatabase.setConnectionProperties(postgresDBConnection);

        DatasourceConnectorSettings settings = new DatasourceConnectorSettings();
        settings.setRoReplica(true);
        Assertions.assertThrows(IllegalArgumentException.class, () -> postgresDatasourceCreator.create(postgresDatabase, settings));
    }

    @Test
    public void testIfdbaaSDataSourceMetricRegisterWasCalled() {
        PostgresDatabase postgresDatabase = new PostgresDatabase();
        postgresDatabase.setName(DB_NAME);

        PostgresDBConnection postgresDBConnection = new PostgresDBConnection(POSTGRES_TEST_URI + DB_NAME, DB_USER, DB_PASSWORD, ADMIN_ROLE);
        postgresDatabase.setConnectionProperties(postgresDBConnection);
        postgresDatabase.setClassifier(new TreeMap<>(Map.of(SCOPE, SERVICE)));

        DbaaSMetricsRegistrar metricsRegistrar = mock(DbaaSMetricsRegistrar.class);
        PostgresDatasourceCreator processor = new PostgresDatasourceCreator(dbaasDsProperties, metricsRegistrar);
        DatasourceConnectorSettings settings = new DatasourceConnectorSettings();
        settings.setSchema(DB_SCHEMA);
        settings.setConnPropertiesParam(Map.of("initializationFailTimeout", -1));
        processor.create(postgresDatabase, settings);
        Mockito.verify(metricsRegistrar, Mockito.times(1)).registerMetrics(eq(PostgresDBType.INSTANCE), any());
    }

}
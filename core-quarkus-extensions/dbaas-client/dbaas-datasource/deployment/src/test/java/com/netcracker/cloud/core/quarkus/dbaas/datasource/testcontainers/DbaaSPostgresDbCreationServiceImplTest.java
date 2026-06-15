package com.netcracker.cloud.core.quarkus.dbaas.datasource.testcontainers;

import com.netcracker.cloud.core.quarkus.dbaas.datasource.config.properties.DatasourceProperties;
import com.netcracker.cloud.core.quarkus.dbaas.datasource.service.MigrationService;
import com.netcracker.cloud.core.quarkus.dbaas.datasource.service.impl.DbaaSPgLogicalDbProvider;
import com.netcracker.cloud.core.quarkus.dbaas.datasource.service.impl.DbaaSPostgresDbCreationServiceImpl;
import com.netcracker.cloud.dbaas.client.DbaasClient;
import com.netcracker.cloud.dbaas.client.entity.connection.PostgresDBConnection;
import com.netcracker.cloud.dbaas.client.entity.database.PostgresDatabase;
import com.netcracker.cloud.dbaas.client.management.DatabaseConfig;
import com.netcracker.cloud.dbaas.client.management.DbaasDbClassifier;
import com.netcracker.cloud.dbaas.client.service.PostgresqlLogicalDbProvider;
import io.agroal.api.AgroalDataSource;
import io.agroal.api.configuration.AgroalConnectionFactoryConfiguration;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import static com.netcracker.cloud.core.quarkus.dbaas.datasource.CommonTestUtils.TEST_NAMESPACE;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@QuarkusTest
@TestProfile(DbaaSPostgresDbCreationServiceImplTest.Profile.class)
public class DbaaSPostgresDbCreationServiceImplTest {

    @Inject
    DatasourceProperties datasourceProperties;

    @InjectMock
    DbaasClient dbaaSClient;

    @InjectMock
    MigrationService migrationService;

    @Inject
    DbaaSPostgresDbCreationServiceImpl creationService;

    @Test
    public void globalInitialSqlIsRead() {
        Assertions.assertEquals(Profile.GLOBAL_VALUE, datasourceProperties.initialSql().orElseThrow());
    }

    @Test
    public void perDatasourceInitialSqlIsRead() {
        Assertions.assertEquals(
                Profile.PER_DB_VALUE,
                datasourceProperties.datasources().get(Profile.LOGICAL_DB).initialSql().orElseThrow()
        );
    }

    @Test
    void mustUseGlobalInitialSql() {
        // no logicalDbName in classifier → getInitialSql(null) returns global value
        DbaasDbClassifier classifier = getTenantClassifier("test-tenant");
        PostgresDatabase postgresDatabase = mockPostgresDatabase();
        postgresDatabase.setClassifier(new TreeMap<>(classifier.asMap()));
        when(dbaaSClient.getOrCreateDatabase(any(), anyString(), anyMap(), any(DatabaseConfig.class)))
                .thenReturn(postgresDatabase);

        PostgresDatabase result = creationService.getOrCreatePostgresDatabase(classifier);

        AgroalConnectionFactoryConfiguration config = ((AgroalDataSource) result.getConnectionProperties().getDataSource())
                .getConfiguration().connectionPoolConfiguration().connectionFactoryConfiguration();
        Assertions.assertEquals(Profile.GLOBAL_VALUE, config.initialSql());
    }

    @Test
    void mustUsePerDbInitialSqlOverridesGlobal() {
        DbaasDbClassifier classifier = getTenantClassifierWithLogicalDb("test-tenant");
        PostgresDatabase postgresDatabase = mockPostgresDatabase();
        postgresDatabase.setClassifier(new TreeMap<>(classifier.asMap()));
        when(dbaaSClient.getOrCreateDatabase(any(), anyString(), anyMap(), any(DatabaseConfig.class)))
                .thenReturn(postgresDatabase);

        PostgresDatabase result = creationService.getOrCreatePostgresDatabase(classifier);

        AgroalConnectionFactoryConfiguration config = ((AgroalDataSource) result.getConnectionProperties().getDataSource())
                .getConfiguration().connectionPoolConfiguration().connectionFactoryConfiguration();
        Assertions.assertEquals(Profile.PER_DB_VALUE, config.initialSql());
    }

    private PostgresDatabase mockPostgresDatabase() {
        PostgresDBConnection connection = new PostgresDBConnection(
                "jdbc:postgresql://localhost:5432/test_db", "user", "pass", "role");
        PostgresDatabase db = new PostgresDatabase();
        db.setName("test_db");
        db.setConnectionProperties(connection);
        return db;
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
        customparams.put("logicalDbName", "configs");
        params.put("microserviceName", "test-service");
        params.put("tenantId", tenantId);
        params.put("customKeys", customparams);
        return new DbaasDbClassifier(params);
    }

    @ApplicationScoped
    static class TestBeans {
        @Produces
        @Alternative
        @Priority(2)
        @ApplicationScoped
        public DbaasClient dbaasClient() {
            return Mockito.mock(DbaasClient.class);
        }

        @Produces
        @Alternative
        @Priority(2)
        @ApplicationScoped
        public PostgresqlLogicalDbProvider logicalDbProvider(DbaasClient dbaasClient) {
            return new DbaaSPgLogicalDbProvider(dbaasClient);
        }
    }

    @NoArgsConstructor
    public static final class Profile implements QuarkusTestProfile {
        static final String GLOBAL_VALUE = "SET TIME ZONE 'UTC'";
        static final String LOGICAL_DB = "configs";
        static final String PER_DB_VALUE = "SET search_path TO my_schema";

        @Override
        public Map<String, String> getConfigOverrides() {
            Map<String, String> props = new HashMap<>();
            props.put("quarkus.datasource.devservices", "false");
            props.put("cloud.microservice.name", "dbaas-initial-sql-properties-test");
            props.put("cloud.microservice.namespace", TEST_NAMESPACE);
            props.put("quarkus.http.test-port", "0");
            props.put("quarkus.http.test-ssl-port", "0");
            props.put("quarkus.hibernate-orm.active", "false");
            props.put("quarkus.flyway.active", "false");
            props.put("quarkus.dbaas.datasource.initial-sql", GLOBAL_VALUE);
            props.put("quarkus.dbaas.datasources." + LOGICAL_DB + ".initial-sql", PER_DB_VALUE);
            return props;
        }

        @Override
        public boolean disableGlobalTestResources() {
            return true;
        }
    }
}

package org.qubership.cloud.quarkus.dbaas.cassandraclient.it;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.annotation.Nullable;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Inject;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import okhttp3.*;
import okio.Buffer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.qubership.cloud.dbaas.client.DbaaSClientOkHttpImpl;
import org.qubership.cloud.dbaas.client.cassandra.entity.connection.CassandraDBConnection;
import org.qubership.cloud.dbaas.client.cassandra.entity.database.CassandraDatabase;
import org.qubership.cloud.dbaas.client.cassandra.entity.database.type.CassandraDBType;
import org.qubership.cloud.dbaas.client.cassandra.service.CassandraLogicalDbProvider;
import org.qubership.cloud.dbaas.client.management.DatabaseConfig;
import org.qubership.cloud.dbaas.client.management.DbaasDbClassifier;
import org.qubership.cloud.quarkus.dbaas.cassandraclient.DbaaSCassandraClient;
import org.qubership.cloud.quarkus.dbaas.cassandraclient.config.properties.CassandraDatabaseSettings;
import org.qubership.cloud.quarkus.dbaas.cassandraclient.config.properties.CassandraDbConfiguration;
import org.qubership.cloud.quarkus.dbaas.cassandraclient.config.properties.CassandraProperties;
import org.qubership.cloud.quarkus.dbaas.cassandraclient.service.CassandraClientCreation;
import org.qubership.cloud.quarkus.dbaas.cassandraclient.service.CqlSessionCreator;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.SortedMap;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.qubership.cloud.dbaas.client.DbaasConst.SCOPE;
import static org.qubership.cloud.dbaas.client.DbaasConst.SERVICE;

@QuarkusTest
@TestProfile(DbaaSCassandraDbCreationConfigTest.ExactDbPrefixAndRoleTestProfile.class)
public class DbaaSCassandraDbCreationConfigTest {
    public static final String DB_NAME_PREFIX = "quarkus.dbaas.cassandra.api.db-prefix";
    public static final String USER_ROLE = "quarkus.dbaas.cassandra.api.runtime-user-role";
    private static final String REPLICATION_VALUE = "{'class': 'NetworkTopologyStrategy', 'dc1': '1'}";
    private static final String TENANT_REPLICATION_VALUE = "tenant-replication";
    private static final String PHYSICAL_DATABASE_ID = "custom-ph-id";

    @Inject
    CassandraLogicalDbProvider dbProvider;
    @InjectMock
    CqlSessionCreator cqlSessionCreator;

    @Inject
    CassandraClientCreation cassandraClientCreation;

    @Inject
    CassandraProperties config;

    @Test
    public void testExtensionsContainsPrefixAndRole() {
        Optional<String> dbPrefix = config.getCassandraDbCreationConfig().dbaasApiPropertiesConfig.dbPrefix;
        Optional<String> userRole = config.getCassandraDbCreationConfig().dbaasApiPropertiesConfig.runtimeUserRole;
        Assertions.assertNotNull(dbPrefix);
        Assertions.assertEquals("test-prefix", dbPrefix.get());
        Assertions.assertNotNull(userRole);
        Assertions.assertEquals("admin", userRole.get());
    }

    @Test
    public void testCassandraConfig() throws IOException {
        DbaaSCassandraClient dbaaSCassandraClient = mock(DbaaSCassandraClient.class);
        when(dbaaSCassandraClient.getKeyspace()).thenReturn(Optional.empty());
        when(cqlSessionCreator.createSession(any(CassandraDatabase.class))).thenReturn(dbaaSCassandraClient);

        CassandraDbConfiguration serviceDbConfiguration = config.getCassandraDbCreationConfig().getServiceDbConfiguration();
        Optional<String> physicalDatabaseIdOptional = serviceDbConfiguration.getPhysicalDatabaseId();
        Assertions.assertNotNull(physicalDatabaseIdOptional);
        Assertions.assertEquals(PHYSICAL_DATABASE_ID, physicalDatabaseIdOptional.orElseThrow());
        Assertions.assertEquals(REPLICATION_VALUE, serviceDbConfiguration.getDatabaseSettings().replication.get());

        var customProvider = (ExactDbPrefixAndRoleTestProfile.CustomConfigCassandraLogicalDbProvider) dbProvider;

        DbaasDbClassifier serviceClassifier = getServiceClassifier();
        DbaasDbClassifier tenantSettingsClassifier = getTenantClassifier("test-tenant1");
        DbaasDbClassifier tenantNoSettingsClassifier = getTenantClassifier("test-tenant2");

        DatabaseConfig databaseConfig = DatabaseConfig.builder().build();
        TestRequest emptyConfigRequest = getDBaaSRequestForConfig(serviceClassifier.asMap(), databaseConfig);
        Assertions.assertNull(emptyConfigRequest.getSettings());

        cassandraClientCreation.getOrCreateCassandraDatabase(serviceClassifier);
        TestRequest serviceConfigRequest = getDBaaSRequestForConfig(serviceClassifier.asMap(), customProvider.getUsedParams());
        Assertions.assertEquals(PHYSICAL_DATABASE_ID, customProvider.getUsedParams().getPhysicalDatabaseId());
        Assertions.assertEquals(REPLICATION_VALUE, ((CassandraDatabaseSettings.CassandraSettingsDTO) customProvider.getUsedParams().getDatabaseSettings()).replication());
        Assertions.assertNotNull(serviceConfigRequest.getSettings());
        Assertions.assertEquals(REPLICATION_VALUE, serviceConfigRequest.getSettings().replication());

        cassandraClientCreation.getOrCreateCassandraDatabase(tenantSettingsClassifier);
        TestRequest tenantSettingsConfigRequest = getDBaaSRequestForConfig(tenantSettingsClassifier.asMap(), customProvider.getUsedParams());
        Assertions.assertNull(customProvider.getUsedParams().getPhysicalDatabaseId());
        Assertions.assertEquals(TENANT_REPLICATION_VALUE, ((CassandraDatabaseSettings.CassandraSettingsDTO) customProvider.getUsedParams().getDatabaseSettings()).replication());
        Assertions.assertNotNull(tenantSettingsConfigRequest.getSettings());
        Assertions.assertEquals(TENANT_REPLICATION_VALUE, tenantSettingsConfigRequest.getSettings().replication());

        cassandraClientCreation.getOrCreateCassandraDatabase(tenantNoSettingsClassifier);
        TestRequest tenantNoSettingsConfigRequest = getDBaaSRequestForConfig(tenantNoSettingsClassifier.asMap(), customProvider.getUsedParams());
        Assertions.assertNull(customProvider.getUsedParams().getPhysicalDatabaseId());
        Assertions.assertNull(customProvider.getUsedParams().getDatabaseSettings());
        Assertions.assertNull(tenantNoSettingsConfigRequest.getSettings());
    }

    private static TestRequest getDBaaSRequestForConfig(Map<String, Object> classifier, DatabaseConfig databaseConfig) throws IOException {
        ObjectMapper JACK = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        OkHttpClient okHttpClient = mock(OkHttpClient.class);
        Call callMock = mock(Call.class);
        Response response = new Response.Builder()
                .code(200)
                .request(new Request.Builder().url("http://localhost").build())
                .protocol(Protocol.HTTP_1_1)
                .message("123")
                .body(ResponseBody.create("{}", MediaType.parse("application/json")))
                .build();
        Buffer bufferedSink = new Buffer();
        when(okHttpClient.newCall(argThat(argument -> {
            try {
                argument.body().writeTo(bufferedSink);
                return true;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }))).thenReturn(callMock);
        when(callMock.execute()).thenReturn(response);
        DbaaSClientOkHttpImpl dbaaSClientOkHttp = new DbaaSClientOkHttpImpl("http://123", okHttpClient);
        dbaaSClientOkHttp.getOrCreateDatabase(CassandraDBType.INSTANCE, "namespace", classifier, databaseConfig);
        String body = bufferedSink.readUtf8();
        return JACK.readValue(body, TestRequest.class);
    }

    private static Map<String, String> getBaseProperties() {
        Map<String, String> properties = new HashMap<>();

        properties.put("quarkus.datasource.devservices", "false");
        properties.put("cloud.microservice.name", "dbaas-client-mongo-params-test");
        properties.put("cloud.microservice.namespace", "test-namespace");
        properties.put("quarkus.http.test-port", "0");
        properties.put("quarkus.http.test-ssl-port", "0");
        properties.put("quarkus.cassandra.contact-points", "test");
        properties.put("quarkus.dbaas.cassandra.api.service.database-settings.replication", REPLICATION_VALUE);
        properties.put("quarkus.dbaas.cassandra.api.tenant.test-tenant1.database-settings.replication", TENANT_REPLICATION_VALUE);
        properties.put("quarkus.dbaas.cassandra.api.service.physical-database-id", PHYSICAL_DATABASE_ID);

        return properties;
    }

    private DbaasDbClassifier getServiceClassifier() {
        Map<String, Object> params = new HashMap<>();
        params.put("microserviceName", "test-service");
        params.put("namespace", "namespace");
        params.put("dbClassifier", "default");
        params.put(SCOPE, SERVICE);
        return new DbaasDbClassifier(params);
    }

    private DbaasDbClassifier getTenantClassifier(String tenantId) {
        DbaasDbClassifier classifier = getServiceClassifier();
        classifier.putProperty("tenantId", tenantId);
        return classifier;
    }

    @NoArgsConstructor
    protected static final class ExactDbPrefixAndRoleTestProfile implements QuarkusTestProfile {
        protected static final String DB_PREFIX_PROPERTY_VAL = "test-prefix";
        protected static final String USER_ROLE_VAL = "admin";

        @Override
        public Map<String, String> getConfigOverrides() {
            Map<String, String> properties = getBaseProperties();
            properties.put(DB_NAME_PREFIX, DB_PREFIX_PROPERTY_VAL);
            properties.put(USER_ROLE, USER_ROLE_VAL);
            return properties;
        }

        @Alternative
        @Priority(2)
        @ApplicationScoped
        public static class CustomConfigCassandraLogicalDbProvider extends CassandraLogicalDbProvider {
            @Getter
            private DatabaseConfig usedParams;

            @Override
            public CassandraDatabase provide(SortedMap<String, Object> classifier, DatabaseConfig params, String namespace) {
                this.usedParams = params;
                CassandraDatabase cassandraDatabase = new CassandraDatabase();
                cassandraDatabase.setClassifier(classifier);
                cassandraDatabase.setConnectionProperties(new CassandraDBConnection());
                return cassandraDatabase;
            }

            @Override
            public @Nullable CassandraConnectionProperty provideConnectionProperty(SortedMap<String, Object> classifier, DatabaseConfig params) {
                return null;
            }
        }
    }

    @Data
    private static class TestRequest {
        private CassandraDatabaseSettings.CassandraSettingsDTO settings;
    }
}


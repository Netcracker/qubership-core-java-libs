package org.qubership.cloud.quarkus.dbaas.mongoclient;

import com.mongodb.client.MongoClient;
import org.qubership.cloud.framework.contexts.tenant.TenantContextObject;
import org.qubership.cloud.context.propagation.core.ContextManager;
import org.qubership.cloud.dbaas.client.management.DbaasDbClassifier;
import org.qubership.cloud.quarkus.dbaas.mongoclient.classifier.TenantClassifierBuilder;
import org.qubership.cloud.quarkus.dbaas.mongoclient.entity.connection.MongoDBConnection;
import org.qubership.cloud.quarkus.dbaas.mongoclient.entity.database.MongoDatabase;
import org.qubership.cloud.quarkus.dbaas.mongoclient.service.MongoClientCreation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import static org.qubership.cloud.framework.contexts.tenant.TenantProvider.TENANT_CONTEXT_NAME;
import static org.qubership.cloud.quarkus.dbaas.mongoclient.CommonMongoTestPart.prepareMongoDbConnection;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DbaaSTenantMongoClientTest {

    private static DbaaSMongoClient dbaasDataSource;
    private static final MongoClientCreation mongoClientCreation = mock(MongoClientCreation.class);
    private static final MongoClient client = mock(MongoClient.class);
    private static MongoDatabase mongoDatabase;

    @BeforeEach
    void prepare() {
        Map<String, Object> params = new HashMap<>();
        params.put("microserviceName", "test-service");
        params.put("dbClassifier", "default");
        dbaasDataSource = new DbaaSMongoClient(new TenantClassifierBuilder(params), mongoClientCreation);
        MongoDBConnection mongoDBConnection = prepareMongoDbConnection();
        mongoDBConnection.setClient(client);

        mongoDatabase = new MongoDatabase();
        mongoDatabase.setConnectionProperties(mongoDBConnection);

        ContextManager.set(TENANT_CONTEXT_NAME, new TenantContextObject("test-tenant"));
    }

    @Test
    public void mustReturnSameTenantMongoDatabase() throws SQLException {
        Map<String, Object> params = new HashMap<>();
        params.put("microserviceName", "test-service");
        params.put("dbClassifier", "default");
        params.put("tenantId", ((TenantContextObject) ContextManager.get(TENANT_CONTEXT_NAME)).getTenant());
        DbaasDbClassifier classifier = new DbaasDbClassifier(params);
        SortedMap<String, Object> newMap = new TreeMap<>(classifier.asMap());
        mongoDatabase.setClassifier(newMap);
        when(mongoClientCreation.getOrCreateMongoDatabase(any())).thenReturn(mongoDatabase);

        MongoClient firstDb = dbaasDataSource.getOrCreateMongoDb().getConnectionProperties().getClient();
        assertNotNull(firstDb);

        MongoClient secondDb = dbaasDataSource.getOrCreateMongoDb().getConnectionProperties().getClient();
        assertEquals(firstDb, secondDb);
    }

    @Test
    public void testTenantClassifierWithoutTenantId() throws SQLException {
        ContextManager.set(TENANT_CONTEXT_NAME, new TenantContextObject((String)null));
        try {
            dbaasDataSource.getOrCreateMongoDb();
        } catch (Exception e) {
            assertEquals("Tenant is not set", e.getMessage());
        }
    }

}
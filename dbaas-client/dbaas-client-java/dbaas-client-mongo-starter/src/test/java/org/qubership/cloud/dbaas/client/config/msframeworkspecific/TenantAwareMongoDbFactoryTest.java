package org.qubership.cloud.dbaas.client.config.msframeworkspecific;

import com.mongodb.client.MongoClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.qubership.cloud.context.propagation.core.ContextManager;
import org.qubership.cloud.dbaas.client.config.msframeworkspecific.testconfig.TestConstants;
import org.qubership.cloud.dbaas.client.config.msframeworkspecific.testconfig.TestMongoDbConfiguration;
import org.qubership.cloud.dbaas.client.entity.connection.MongoDBConnection;
import org.qubership.cloud.dbaas.client.entity.database.MongoDatabase;
import org.qubership.cloud.dbaas.client.management.DatabasePool;
import org.qubership.cloud.framework.contexts.tenant.TenantContextObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.TreeMap;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.qubership.cloud.dbaas.client.config.DbaasMongoConfiguration.TENANT_MONGO_TEMPLATE;
import static org.qubership.cloud.dbaas.client.config.msframeworkspecific.testconfig.TestMongoDbConfiguration.TENANT_ID;
import static org.qubership.cloud.framework.contexts.tenant.BaseTenantProvider.TENANT_CONTEXT_NAME;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TestMongoDbConfiguration.class)
public class TenantAwareMongoDbFactoryTest {

    @Autowired
    @Qualifier(TENANT_MONGO_TEMPLATE)
    MongoTemplate tenantAwareMongoTemplate;

    @Autowired
    private MongoClient fongoClient;

    @Autowired
    private DatabasePool databasePool;

    @BeforeEach
    public void setUp() throws Exception {
        ContextManager.clear(TENANT_CONTEXT_NAME);
        ContextManager.set(TENANT_CONTEXT_NAME, new TenantContextObject(TENANT_ID));

        MongoDatabase database = new MongoDatabase();
        database.setName(TestConstants.AUTH_DB_NAME);

        MongoDBConnection connection = new MongoDBConnection();
        connection.setUrl(TestConstants.MONGO_TEST_URI + TestConstants.AUTH_DB_NAME);
        connection.setUsername(TestConstants.DB_USER);
        connection.setAuthDbName(TestConstants.DB_NAME);
        connection.setPassword(TestConstants.DB_PASSWORD);
        database.setConnectionProperties(connection);
        connection.setClient(fongoClient);
        database.setClassifier(new TreeMap<>());

        when(databasePool.getOrCreateDatabase(any(), any(), any())).thenReturn(database);
    }

    @Test
    public void testGetDb_Default() throws Exception {
        com.mongodb.client.MongoDatabase mongoDb = tenantAwareMongoTemplate.getDb();
        Assertions.assertEquals(TestConstants.AUTH_DB_NAME, mongoDb.getName());
    }
}
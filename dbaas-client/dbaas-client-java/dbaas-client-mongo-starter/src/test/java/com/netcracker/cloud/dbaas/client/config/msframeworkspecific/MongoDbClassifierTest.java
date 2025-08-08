package com.netcracker.cloud.dbaas.client.config.msframeworkspecific;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import com.netcracker.cloud.context.propagation.core.ContextManager;
import com.netcracker.cloud.dbaas.client.DbaasConst;
import com.netcracker.cloud.dbaas.client.config.MSInfoProvider;
import com.netcracker.cloud.dbaas.client.config.msframeworkspecific.testconfig.TestMongoDbConfiguration;
import com.netcracker.cloud.dbaas.client.entity.database.type.MongoDBType;
import com.netcracker.cloud.dbaas.client.management.DatabaseConfig;
import com.netcracker.cloud.dbaas.client.management.DatabasePool;
import com.netcracker.cloud.dbaas.client.management.DbaasDbClassifier;
import com.netcracker.cloud.framework.contexts.tenant.TenantContextObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.TestContextManager;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static com.netcracker.cloud.dbaas.client.DbaasConst.SCOPE;
import static com.netcracker.cloud.dbaas.client.DbaasConst.SERVICE;
import static com.netcracker.cloud.dbaas.client.config.DbaasMongoConfiguration.SERVICE_MONGO_TEMPLATE;
import static com.netcracker.cloud.dbaas.client.config.DbaasMongoConfiguration.TENANT_MONGO_TEMPLATE;
import static com.netcracker.cloud.dbaas.client.config.msframeworkspecific.testconfig.TestMongoDbConfiguration.TENANT_ID;
import static com.netcracker.cloud.framework.contexts.tenant.BaseTenantProvider.TENANT_CONTEXT_NAME;

public class MongoDbClassifierTest {
    private static final String LOCALDEV_KEY = "localdev";
    private static final String LOCALDEV_NAMESPACE = "127.0.0.1.xip.io";
    private static final String DB_CLASSIFIER_KEY = "dbClassifier";
    private static final String DB_CLASSIFIER = "test-db";

    @Autowired
    private MSInfoProvider msInfoProvider;

    @Autowired
    @Qualifier(TENANT_MONGO_TEMPLATE)
    private MongoTemplate tenantMongoTemplate;

    @Autowired
    @Qualifier(SERVICE_MONGO_TEMPLATE)
    private MongoTemplate serviceMongoTemplate;

    @Autowired
    DatabasePool dbaasConnectionPoolMock;

    @BeforeEach
    public void setUp() {
        ContextManager.set(TENANT_CONTEXT_NAME, new TenantContextObject(TENANT_ID));
    }

    private void initContext(Class<?> testClass) throws Exception {
        TestContextManager testContextManager = new TestContextManager(testClass);
        testContextManager.prepareTestInstance(this);
    }

    private Map<String, Object> getTenantClassifier() {
        Mockito.reset(dbaasConnectionPoolMock);
        final ArgumentCaptor<DbaasDbClassifier> dbClassifierCaptor = ArgumentCaptor.forClass(DbaasDbClassifier.class);

        try {
            tenantMongoTemplate.getDb();
        } catch (NullPointerException e) {
            // expected exception on mock
        }

        Mockito.verify(dbaasConnectionPoolMock).getOrCreateDatabase(eq(MongoDBType.INSTANCE), dbClassifierCaptor.capture(), eq(DatabaseConfig.builder().build()));
        return dbClassifierCaptor.getValue().asMap(); // get captured db classifier
    }

    private Map<String, Object> getServiceClassifier() {
        Mockito.reset(dbaasConnectionPoolMock);
        final ArgumentCaptor<DbaasDbClassifier> dbClassifierCaptor = ArgumentCaptor.forClass(DbaasDbClassifier.class);

        try {
            serviceMongoTemplate.getDb();
        } catch (NullPointerException e) {
            // expected exception on mock
        }

        Mockito.verify(dbaasConnectionPoolMock).getOrCreateDatabase(eq(MongoDBType.INSTANCE), dbClassifierCaptor.capture(), eq(DatabaseConfig.builder().build()));
        return dbClassifierCaptor.getValue().asMap(); // get captured db classifier
    }

    @Test
    public void testLocalDev() throws Exception {
        initContext(CustomConfigTest.class);

        Mockito.when(msInfoProvider.getLocalDevNamespace()).thenReturn(LOCALDEV_NAMESPACE);

        Map<String, Object> serviceClassifier = getServiceClassifier();
        Map<String, Object> tenantClassifier = getTenantClassifier();

        assertEquals(LOCALDEV_NAMESPACE, serviceClassifier.get(LOCALDEV_KEY));
        assertEquals(SERVICE, serviceClassifier.get(SCOPE));
        assertEquals(DB_CLASSIFIER, serviceClassifier.get(DB_CLASSIFIER_KEY));

        assertEquals(LOCALDEV_NAMESPACE, tenantClassifier.get(LOCALDEV_KEY));
        assertEquals(TENANT_ID, tenantClassifier.get(DbaasConst.TENANT_ID));
        assertEquals(DB_CLASSIFIER, tenantClassifier.get(DB_CLASSIFIER_KEY));
    }

    @Test
    public void testLaunchInCloud() throws Exception {
        initContext(DefaultConfigTest.class);

        Mockito.when(msInfoProvider.getLocalDevNamespace()).thenReturn(null);

        Map<String, Object> serviceClassifier = getServiceClassifier();
        Map<String, Object> tenantClassifier = getTenantClassifier();

        assertFalse(serviceClassifier.containsKey(LOCALDEV_KEY));
        assertEquals(SERVICE, serviceClassifier.get(SCOPE));
        assertEquals("default", serviceClassifier.get(DB_CLASSIFIER_KEY));

        assertFalse(tenantClassifier.containsKey(LOCALDEV_KEY));
        assertEquals(TENANT_ID, tenantClassifier.get(DbaasConst.TENANT_ID));
        assertEquals("default", tenantClassifier.get(DB_CLASSIFIER_KEY));
    }

    @ExtendWith(SpringExtension.class)
    @Import(TestMongoDbConfiguration.class)
    @TestPropertySource(properties = {
            "spring.application.namespace=cloud-catalog-test",
            "cloud.microservice.namespace=unknown",
            "cloud.microservice.name=test-ms",
            "dbaas.mongo.dbClassifier=test-db"
    })
    private static class CustomConfigTest {
    }

    @ExtendWith(SpringExtension.class)
    @Import(TestMongoDbConfiguration.class)
    @TestPropertySource(properties = {
            "spring.application.namespace=cloud-catalog-test",
            "cloud.microservice.namespace=unknown",
            "cloud.microservice.name=test-ms",
            "dbaas.mongo.dbClassifier=default"
    })
    private static class DefaultConfigTest {
    }
}

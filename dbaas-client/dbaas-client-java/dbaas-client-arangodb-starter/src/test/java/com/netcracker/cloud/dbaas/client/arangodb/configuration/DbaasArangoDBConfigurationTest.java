package com.netcracker.cloud.dbaas.client.arangodb.configuration;

import com.arangodb.springframework.core.template.ArangoTemplate;
import com.netcracker.cloud.framework.contexts.tenant.context.TenantContext;
import com.netcracker.cloud.dbaas.client.arangodb.entity.database.type.ArangoDBType;
import com.netcracker.cloud.dbaas.client.arangodb.test.configuration.TestArangoDBConfiguration;
import com.netcracker.cloud.dbaas.client.entity.DbaasApiProperties;
import com.netcracker.cloud.dbaas.client.management.ArangoDatabaseProvider;
import com.netcracker.cloud.dbaas.client.management.DatabasePool;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static com.netcracker.cloud.dbaas.client.arangodb.configuration.ServiceArangoTemplateConfiguration.*;
import static com.netcracker.cloud.dbaas.client.arangodb.configuration.ServiceDbaasArangoConfiguration.*;
import static com.netcracker.cloud.dbaas.client.arangodb.configuration.TenantArangoTemplateConfiguration.*;
import static com.netcracker.cloud.dbaas.client.arangodb.configuration.TenantDbaasArangoConfiguration.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {TestArangoDBConfiguration.class, TenantArangoTemplateConfiguration.class})
@TestPropertySource(properties = {
        "dbaas.arangodb.dbName=db-test-name-1",
        "dbaas.arangodb.acquireHostList=true",
        "dbaas.arangodb.acquireHostListInterval=12345",
        "dbaas.api.arangodb.db-prefix=test-prefix",
        "dbaas.api.arangodb.runtime-user-role=admin"
})
class DbaasArangoDBConfigurationTest {

    @SpyBean
    private DatabasePool databasePool;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    @Qualifier(TENANT_ARANGODB_PROVIDER)
    private ArangoDatabaseProvider tenantArango;

    @Autowired
    @Qualifier(SERVICE_ARANGODB_PROVIDER)
    private ArangoDatabaseProvider serviceArango;

    @Autowired
    private ArangoTemplate primaryArangoTemplate;

    @Autowired
    private DbaasArangoDBConfigurationProperties arangoConfig;

    @Autowired
    private DbaasApiProperties arangodbDbaasApiProperties;

    @Test
    void checkArangoDatabaseProvider() {
        Map<String, ArangoDatabaseProvider> beans = applicationContext.getBeansOfType(ArangoDatabaseProvider.class);
        assertTrue(beans.containsKey(SERVICE_ARANGODB_PROVIDER));
        assertTrue(beans.containsKey(TENANT_ARANGODB_PROVIDER));
    }

    @Test
    void checkArangoTemplateBeans() {
        Map<String, ArangoTemplate> beans = applicationContext.getBeansOfType(ArangoTemplate.class);
        assertTrue(beans.containsKey(SERVICE_ARANGO_TEMPLATE));
        assertTrue(beans.containsKey(TENANT_ARANGO_TEMPLATE));
    }

    @Test
    void checkServiceArangoTemplateIsPrimary() {
        Map<String, ArangoTemplate> beans = applicationContext.getBeansOfType(ArangoTemplate.class);
        assertEquals(beans.get(SERVICE_ARANGO_TEMPLATE), primaryArangoTemplate);
    }

    @Test
    void checkArangoConfigLoaded() {
        assertEquals("db-test-name-1", arangoConfig.getArangodb().get("dbName"));
        assertEquals("true", arangoConfig.getArangodb().get("acquireHostList"));
        assertEquals("12345", arangoConfig.getArangodb().get("acquireHostListInterval"));
    }

    @Test
    void checkIfDatabaseTypeIsArangoDBInServiceClient() {
        serviceArango.provide("default");
        // 2 Invocations expected: initial attempt and retry because arango database is not up
        Mockito.verify(databasePool, Mockito.times(2)).getOrCreateDatabase(eq(ArangoDBType.INSTANCE), any(), any());
        Mockito.clearInvocations(databasePool);
    }

    @Test
    void checkIfDatabaseTypeIsArangoDBInTenantClient() {
        TenantContext.set("test-tenant");
        tenantArango.provide("default");
        // 2 Invocations expected: initial attempt and retry because arango database is not up
        Mockito.verify(databasePool, Mockito.times(2)).getOrCreateDatabase(eq(ArangoDBType.INSTANCE), any(), any());
        Mockito.clearInvocations(databasePool);
    }

    @Test
    public void checkArangoApiProperties() {
        Assertions.assertEquals("admin", arangodbDbaasApiProperties.getRuntimeUserRole());
        Assertions.assertEquals("test-prefix", arangodbDbaasApiProperties.getDbPrefix());
    }

}

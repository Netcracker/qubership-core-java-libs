package org.qubership.cloud.dbaas.client.opensearch;

import org.qubership.cloud.framework.contexts.tenant.context.TenantContext;
import org.qubership.cloud.dbaas.client.management.DatabaseConfig;
import org.qubership.cloud.dbaas.client.management.DatabasePool;
import org.qubership.cloud.dbaas.client.opensearch.entity.*;
import org.qubership.cloud.dbaas.client.opensearch.management.OpensearchPostConnectProcessor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.qubership.cloud.dbaas.client.opensearch.entity.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;

import java.util.Collections;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.qubership.cloud.dbaas.client.opensearch.config.DbaasOpensearchConfiguration.*;

@SpringBootTest(properties = {
        "dbaas.api.opensearch.runtime-user-role=rw"
        }
)
@ContextConfiguration(classes = {TestOpensearchConfiguration.class})
public class DbaasOpensearchConfigurationTest {

    @Autowired
    private DatabasePool databasePool;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    @Qualifier(TENANT_NATIVE_OPENSEARCH_CLIENT)
    private DbaasOpensearchClient tenantClient;

    @Autowired
    @Qualifier(SERVICE_NATIVE_OPENSEARCH_CLIENT)
    private DbaasOpensearchClient serviceClient;

    @Autowired
    private OpensearchProperties opensearchProperties;

    @BeforeEach
    public void setUp() {
        OpensearchIndex opensearchIndex = mock(OpensearchIndex.class);
        when(databasePool.getOrCreateDatabase(any(), any(), any())).thenReturn(opensearchIndex);
        OpensearchIndexConnection opensearchIndexConnection = mock(OpensearchIndexConnection.class);
        when(opensearchIndex.getConnectionProperties()).thenReturn(opensearchIndexConnection);
        OpenSearchClient restHighLevelClient = mock(OpenSearchClient.class);
        when(opensearchIndexConnection.getOpenSearchClient()).thenReturn(restHighLevelClient);
    }

    @Test
    public void checkDbaasRestHighLevelClient() {
        Map<String, DbaasOpensearchClient> beans = applicationContext.getBeansOfType(DbaasOpensearchClient.class);
        Assertions.assertEquals(2, beans.size());
    }

    @Test
    public void checkOpensearchPostConnectProcessorBean() {
        Assertions.assertNotNull(applicationContext.getBeansOfType(OpensearchPostConnectProcessor.class));
    }

    @Test
    public void checkIfDatabaseTypeIsOpensearchInServiceClient() {
        serviceClient.getClient();
        Mockito.verify(databasePool).getOrCreateDatabase(eq(OpensearchDBType.INSTANCE),
                any(), any());
        Mockito.clearInvocations(databasePool);
    }

    @Test
    public void checkIfDatabaseTypeIsOpensearchInTenantClient() {
        TenantContext.set("test-tenant");
        serviceClient.getClient();
        Mockito.verify(databasePool).getOrCreateDatabase(eq(OpensearchDBType.INSTANCE),
                any(), any());
        Mockito.clearInvocations(databasePool);
    }

    @Test
    public void checkOpensearchPropertiesRole() {
        Assertions.assertEquals("rw", opensearchProperties.getRuntimeUserRole());
    }

    @Test
    public void checkUserRoleSerivceClientDatabasePool() {
        serviceClient.getClient();
        OpensearchDatabaseSettings opensearchDatabaseSettings = new OpensearchDatabaseSettings();
        opensearchDatabaseSettings.setResourcePrefix(true);
        opensearchDatabaseSettings.setCreateOnly(Collections.singletonList("user"));
        DatabaseConfig config = DatabaseConfig.builder().databaseSettings(opensearchDatabaseSettings).userRole("rw").build();
        Mockito.verify(databasePool).getOrCreateDatabase(eq(OpensearchDBType.INSTANCE),
                any(), eq(config));
        Mockito.clearInvocations(databasePool);
    }

    @Test
    public void checkUserRoleTenantClientDatabasePool() {
        serviceClient.getClient();
        OpensearchDatabaseSettings opensearchDatabaseSettings = new OpensearchDatabaseSettings();
        opensearchDatabaseSettings.setResourcePrefix(true);
        opensearchDatabaseSettings.setCreateOnly(Collections.singletonList("user"));
        DatabaseConfig config = DatabaseConfig.builder().databaseSettings(opensearchDatabaseSettings).userRole("rw").build();
        Mockito.verify(databasePool).getOrCreateDatabase(eq(OpensearchDBType.INSTANCE),
                any(), eq(config));
        Mockito.clearInvocations(databasePool);
    }


}

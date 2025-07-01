package org.qubership.cloud.dbaas.client.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.qubership.cloud.context.propagation.core.ContextManager;
import org.qubership.cloud.dbaas.client.DbaasConst;
import org.qubership.cloud.dbaas.client.management.DbaasPostgresProxyDataSource;
import org.qubership.cloud.framework.contexts.tenant.TenantContextObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.sql.DataSource;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.qubership.cloud.dbaas.client.DbaasConst.SCOPE;
import static org.qubership.cloud.dbaas.client.DbaasConst.SERVICE;
import static org.qubership.cloud.dbaas.client.config.DbaasPostgresConfiguration.SERVICE_POSTGRES_DATASOURCE;
import static org.qubership.cloud.dbaas.client.config.DbaasPostgresConfiguration.TENANT_POSTGRES_DATASOURCE;
import static org.qubership.cloud.framework.contexts.tenant.BaseTenantProvider.TENANT_CONTEXT_NAME;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = PostgresDbTestContext.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class MicroservicePostgresDbConfigurationTest {

    private static final String LOCALDEV_KEY = "localdev";
    private static final String LOCALDEV_NAMESPACE = "127.0.0.1.xip.io";
    private static final String TENANT_ID = "test-tenant";

    @Autowired
    private MSInfoProvider msInfoProvider;

    @Autowired
    @Qualifier(TENANT_POSTGRES_DATASOURCE)
    private DataSource tenantAwareDataSource;

    @Autowired
    @Qualifier(SERVICE_POSTGRES_DATASOURCE)
    private DataSource serviceDataSource;

    @BeforeEach
    public void setUp() {
        ContextManager.set(TENANT_CONTEXT_NAME, new TenantContextObject(TENANT_ID));
    }

    private Map<String, Object> getTenantClassifier() {
        return ((DbaasPostgresProxyDataSource) tenantAwareDataSource).getDatabase().getClassifier();
    }

    private Map<String, Object> getServiceClassifier() {
        return ((DbaasPostgresProxyDataSource) serviceDataSource).getDatabase().getClassifier();
    }

    @Test
    public void testLocalDev() throws Exception {
        Mockito.when(msInfoProvider.getLocalDevNamespace()).thenReturn(LOCALDEV_NAMESPACE);

        Map<String, Object> serviceClassifier = getServiceClassifier();
        Map<String, Object> tenantClassifier = getTenantClassifier();

        assertEquals(LOCALDEV_NAMESPACE, serviceClassifier.get(LOCALDEV_KEY));
        assertEquals(SERVICE, serviceClassifier.get(SCOPE));

        assertEquals(LOCALDEV_NAMESPACE, tenantClassifier.get(LOCALDEV_KEY));
        assertEquals(TENANT_ID, tenantClassifier.get(DbaasConst.TENANT_ID));
    }

    @Test
    public void testLaunchInCloud() throws Exception {
        Mockito.when(msInfoProvider.getLocalDevNamespace()).thenReturn(null);

        Map<String, Object> serviceClassifier = getServiceClassifier();
        Map<String, Object> tenantClassifier = getTenantClassifier();

        assertFalse(serviceClassifier.containsKey(LOCALDEV_KEY));
        assertEquals(SERVICE, serviceClassifier.get(SCOPE));

        assertFalse(tenantClassifier.containsKey(LOCALDEV_KEY));
        assertEquals(TENANT_ID, tenantClassifier.get(DbaasConst.TENANT_ID));
    }
}

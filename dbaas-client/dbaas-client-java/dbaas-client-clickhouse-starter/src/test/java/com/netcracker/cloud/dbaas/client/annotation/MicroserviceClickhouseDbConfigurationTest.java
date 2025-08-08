package com.netcracker.cloud.dbaas.client.annotation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.qubership.cloud.context.propagation.core.ContextManager;
import org.qubership.cloud.dbaas.client.DbaasConst;
import org.qubership.cloud.dbaas.client.config.MSInfoProvider;
import org.qubership.cloud.dbaas.client.management.DbaasClickhouseDatasource;
import org.qubership.cloud.framework.contexts.tenant.TenantContextObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.sql.DataSource;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.qubership.cloud.dbaas.client.DbaasConst.SCOPE;
import static org.qubership.cloud.dbaas.client.DbaasConst.SERVICE;
import static org.qubership.cloud.dbaas.client.config.DbaasClickhouseConfiguration.SERVICE_CLICKHOUSE_DATASOURCE;
import static org.qubership.cloud.dbaas.client.config.DbaasClickhouseConfiguration.TENANT_CLICKHOUSE_DATASOURCE;
import static org.qubership.cloud.framework.contexts.tenant.BaseTenantProvider.TENANT_CONTEXT_NAME;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = ClickhouseDbTestContext.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class MicroserviceClickhouseDbConfigurationTest {

    private static final String LOCALDEV_KEY = "localdev";
    private static final String LOCALDEV_NAMESPACE = "127.0.0.1.xip.io";
    private static final String TENANT_ID = "test-tenant";

    @Autowired
    private MSInfoProvider msInfoProvider;

    @Autowired
    @Qualifier(TENANT_CLICKHOUSE_DATASOURCE)
    private DataSource tenantAwareDataSource;

    @Autowired
    @Qualifier(SERVICE_CLICKHOUSE_DATASOURCE)
    private DataSource serviceDataSource;

    @BeforeEach
    void setUp() {
        ContextManager.set(TENANT_CONTEXT_NAME, new TenantContextObject(TENANT_ID));
    }

    private Map<String, Object> getTenantClassifier() {
        return ((DbaasClickhouseDatasource) tenantAwareDataSource).getDatabase().getClassifier();
    }

    private Map<String, Object> getServiceClassifier() {
        return ((DbaasClickhouseDatasource) serviceDataSource).getDatabase().getClassifier();
    }

    @Test
    void testLocalDev() throws Exception {
        Mockito.when(msInfoProvider.getLocalDevNamespace()).thenReturn(LOCALDEV_NAMESPACE);

        Map<String, Object> serviceClassifier = getServiceClassifier();
        Map<String, Object> tenantClassifier = getTenantClassifier();

        assertEquals(LOCALDEV_NAMESPACE, serviceClassifier.get(LOCALDEV_KEY));
        assertEquals(SERVICE, serviceClassifier.get(SCOPE));

        assertEquals(LOCALDEV_NAMESPACE, tenantClassifier.get(LOCALDEV_KEY));
        assertEquals(TENANT_ID, tenantClassifier.get(DbaasConst.TENANT_ID));
    }

    @Test
    void testLaunchInCloud() throws Exception {
        Mockito.when(msInfoProvider.getLocalDevNamespace()).thenReturn(null);

        Map<String, Object> serviceClassifier = getServiceClassifier();
        Map<String, Object> tenantClassifier = getTenantClassifier();

        assertFalse(serviceClassifier.containsKey(LOCALDEV_KEY));
        assertEquals(SERVICE, serviceClassifier.get(SCOPE));

        assertFalse(tenantClassifier.containsKey(LOCALDEV_KEY));
        assertEquals(TENANT_ID, tenantClassifier.get(DbaasConst.TENANT_ID));
    }
}

package org.qubership.cloud.dbaas.client.management.classifier;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;
import org.qubership.cloud.context.propagation.core.ContextManager;
import org.qubership.cloud.framework.contexts.tenant.TenantContextObject;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.qubership.cloud.dbaas.client.DbaasConst.*;
import static org.qubership.cloud.framework.contexts.tenant.BaseTenantProvider.TENANT_CONTEXT_NAME;

@Slf4j
public class TenantDbaaSClassifierBuilderTest {

    public static final String TEST_TENANT = "test-tenant";
    public static final int THREAD_POOL_SIZE = 500;

    @Test
    void checkTenantSpecificFieldsInClassifier() {
        ContextManager.set(TENANT_CONTEXT_NAME, new TenantContextObject(TEST_TENANT));
        TenantDbaaSClassifierBuilder tenantDbaaSClassifierBuilder = new TenantDbaaSClassifierBuilder(null);
        Map<String, Object> actualClassifier = tenantDbaaSClassifierBuilder.build().asMap();
        log.info(String.valueOf(actualClassifier));
        HashMap<String, Object> expectedClassifier = new HashMap<>();
        expectedClassifier.put(TENANT_ID, TEST_TENANT);
        expectedClassifier.put(SCOPE, TENANT);
        Assertions.assertEquals(expectedClassifier, actualClassifier);
    }

    @Test
    void checkConcurrencyForTenantDbaaSClassifierBuilder() {
        /* create 'DbaasDbClassifier.Builder wrapped' before test concurrency */
        ContextManager.set(TENANT_CONTEXT_NAME, new TenantContextObject(TEST_TENANT));
        TenantDbaaSClassifierBuilder tenantDbaaSClassifierBuilder = new TenantDbaaSClassifierBuilder(null);
        tenantDbaaSClassifierBuilder.build();

        AtomicBoolean failed = new AtomicBoolean(false);
        ExecutorService executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        for (int j = 0; j < THREAD_POOL_SIZE; j++) {
            int id = j;
            executorService.execute(() -> {
                for (int i = 0; i < 500; i++) {
                    String tenantName = "tenant" + id;
                    try {
                        checkTenantDbaaSClassifier(tenantDbaaSClassifierBuilder, tenantName, failed);
                    } catch (AssertionFailedError e) {
                        break;
                    }
                }
            });
        }
        executorService.shutdown();
        Assertions.assertFalse(failed.get());
    }

    private void checkTenantDbaaSClassifier(TenantDbaaSClassifierBuilder tenantDbaaSClassifierBuilder, String tenantName, AtomicBoolean failed) {
        ContextManager.set(TENANT_CONTEXT_NAME, new TenantContextObject(tenantName));
        try {
            Assertions.assertEquals(tenantName, tenantDbaaSClassifierBuilder.build().asMap().get(TENANT_ID));
        } catch (AssertionFailedError e) {
            failed.set(true);
            throw e;
        }
    }
}
package org.qubership.cloud.framework.contexts.tenant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.qubership.cloud.context.propagation.core.ContextManager;
import org.qubership.cloud.context.propagation.core.RequestContextPropagation;
import org.qubership.cloud.context.propagation.core.contextdata.OutgoingContextData;
import org.slf4j.MDC;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.qubership.cloud.framework.contexts.tenant.DefaultTenantProvider.TENANT_CONTEXT_NAME;
import static org.qubership.cloud.framework.contexts.tenant.TenantContextObject.TENANT_HEADER;


class TenantContextTest {
    private static final String TENANT = "123456";

    @BeforeEach
    void setUp() {
        ContextManager.clearAll();
        MDC.clear();
    }

    @Test
    void populateResponse() {
        TenantContextObject tenantContextObject = new TenantContextObject(TENANT);
        ContextManager.set(TENANT_CONTEXT_NAME, tenantContextObject);
        ContextDataResponse contextDataResponse = new ContextDataResponse();
        RequestContextPropagation.populateResponse(contextDataResponse);
        assertEquals(TENANT, contextDataResponse.getResponseHeaders().get(TENANT_HEADER));
    }

    @Test
    void testSetTenantIdInMDC() {
        ContextManager.set(TENANT_CONTEXT_NAME, new TenantContextObject(TENANT));
        String tenantId = MDC.get("tenantId");
        assertEquals(TENANT, tenantId);
    }

    @Test
    void testClearTenantIdInMDC() {
        ContextManager.set(TENANT_CONTEXT_NAME, new TenantContextObject(TENANT));
        String tenantId = MDC.get("tenantId");
        assertEquals(TENANT, tenantId);
        ContextManager.clear(TENANT_CONTEXT_NAME);
        tenantId = MDC.get("tenantId");
        assertNull(tenantId, "Tenant id must be null");
    }

    @Test
    void testSetEmptyTenantIdInMDC() {
        ContextManager.set(TENANT_CONTEXT_NAME, new TenantContextObject(TENANT));
        String tenantId = MDC.get("tenantId");
        assertEquals(TENANT, tenantId);
        ContextManager.set(TENANT_CONTEXT_NAME, new TenantContextObject((String)null));
        tenantId = MDC.get("tenantId");
        assertNull(tenantId, "Tenant id must be null");
    }

    @Test
    void testCreateContextSnapshotIfContextIsEmpty() {
        ContextManager.clearAll();
        assertNotNull(ContextManager.createContextSnapshot());
    }

    @Test
    void testPropagateWithNonEmptyTenant() {
        OutgoingContextData mockOutgoingContextData = mock(OutgoingContextData.class);
        TenantContextObject contextObject = new TenantContextObject(TENANT);
        contextObject.propagate(mockOutgoingContextData);
        verify(mockOutgoingContextData).set(TENANT_HEADER, TENANT);
    }

    @Test
    void testPropagateWithEmptyTenant() {
        OutgoingContextData mockOutgoingContextData = mock(OutgoingContextData.class);
        TenantContextObject contextObject = new TenantContextObject("");
        contextObject.propagate(mockOutgoingContextData);
        assertTrue(contextObject.getTenant().isEmpty());
    }

    public static class ContextDataResponse implements OutgoingContextData {

        private final Map<String, Object> responseHeaders = new HashMap<>();


        @Override
        public void set(String name, Object values) {
            responseHeaders.put(name, values);
        }

        public Map<String, Object> getResponseHeaders() {
            return responseHeaders;
        }
    }
}
package org.qubership.cloud.framework.contexts.tenant;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.qubership.cloud.context.propagation.core.ContextManager;
import org.qubership.cloud.context.propagation.core.RequestContextPropagation;
import org.qubership.cloud.context.propagation.core.contextdata.OutgoingContextData;
import org.slf4j.MDC;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.qubership.cloud.framework.contexts.tenant.DefaultTenantProvider.TENANT_CONTEXT_NAME;
import static org.qubership.cloud.framework.contexts.tenant.TenantContextObject.TENANT_HEADER;


public class TenantContextTest {
    private static final String TENANT = "123456";

    @Before
    public void setUp() {
        ContextManager.clearAll();
        MDC.clear();
    }

    @Test
    public void populateResponse() {
        TenantContextObject tenantContextObject = new TenantContextObject(TENANT);
        ContextManager.set(TENANT_CONTEXT_NAME, tenantContextObject);
        ContextDataResponse contextDataResponse = new ContextDataResponse();
        RequestContextPropagation.populateResponse(contextDataResponse);
        assertEquals(TENANT, contextDataResponse.getResponseHeaders().get(TENANT_HEADER));
    }

    @Test
    public void testSetTenantIdInMDC() {
        ContextManager.set(TENANT_CONTEXT_NAME, new TenantContextObject(TENANT));
        String tenantId = MDC.get("tenantId");
        assertEquals(TENANT, tenantId);
    }

    @Test
    public void testClearTenantIdInMDC() {
        ContextManager.set(TENANT_CONTEXT_NAME, new TenantContextObject(TENANT));
        String tenantId = MDC.get("tenantId");
        assertEquals(TENANT, tenantId);
        ContextManager.clear(TENANT_CONTEXT_NAME);
        tenantId = MDC.get("tenantId");
        Assert.assertNull("Tenant id must be null", tenantId);
    }

    @Test
    public void testSetEmptyTenantIdInMDC() {
        ContextManager.set(TENANT_CONTEXT_NAME, new TenantContextObject(TENANT));
        String tenantId = MDC.get("tenantId");
        assertEquals(TENANT, tenantId);
        ContextManager.set(TENANT_CONTEXT_NAME, new TenantContextObject((String)null));
        tenantId = MDC.get("tenantId");
        Assert.assertNull("Tenant id must be null", tenantId);
    }

    @Test
    public void testCreateContextSnapshotIfContextIsEmpty() {
        ContextManager.clearAll();
        Assert.assertNotNull(ContextManager.createContextSnapshot());
    }

    @Test
    public void testPropagateWithNonEmptyTenant() {
        OutgoingContextData mockOutgoingContextData = mock(OutgoingContextData.class);
        TenantContextObject contextObject = new TenantContextObject(TENANT);
        contextObject.propagate(mockOutgoingContextData);
        verify(mockOutgoingContextData).set(TENANT_HEADER, TENANT);
    }

    @Test
    public void testPropagateWithEmptyTenant() {
        OutgoingContextData mockOutgoingContextData = mock(OutgoingContextData.class);
        TenantContextObject contextObject = new TenantContextObject("");
        contextObject.propagate(mockOutgoingContextData);
        assertTrue(contextObject.getTenant().isEmpty());
    }

    public static class ContextDataResponse implements OutgoingContextData {

        private Map<String, Object> responseHeaders = new HashMap<>();


        @Override
        public void set(String name, Object values) {
            responseHeaders.put(name, values);
        }

        public Map<String, Object> getResponseHeaders() {
            return responseHeaders;
        }
    }
}
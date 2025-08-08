package com.netcracker.cloud.framework.contexts.tenant.context;

import com.netcracker.cloud.context.propagation.core.ContextManager;
import com.netcracker.cloud.framework.contexts.tenant.TenantContextObject;
import com.netcracker.cloud.framework.contexts.tenant.TenantNotFoundException;
import static com.netcracker.cloud.framework.contexts.tenant.DefaultTenantProvider.TENANT_CONTEXT_NAME;

import java.util.Optional;


public class TenantContext {

    /**
     * This method is for general use
     *
     * @return String tenant
     * @throws TenantNotFoundException
     */
    public static String get() {
        return ((TenantContextObject) ContextManager.get(TENANT_CONTEXT_NAME)).getTenant();
    }

    /**
     * Use this when catching an exception is not possible
     *
     * @return String tenant
     */
    public static String getWithoutException() {
        Optional<TenantContextObject> tenant = ContextManager.getSafe(TENANT_CONTEXT_NAME);
        return tenant.map(TenantContextObject::getTenant).orElse(null);
    }

    public static void set(String tenant) {
        ContextManager.set(TENANT_CONTEXT_NAME, new TenantContextObject(tenant));
    }

    public static void clear() {
        ContextManager.clear(TENANT_CONTEXT_NAME);
    }
}

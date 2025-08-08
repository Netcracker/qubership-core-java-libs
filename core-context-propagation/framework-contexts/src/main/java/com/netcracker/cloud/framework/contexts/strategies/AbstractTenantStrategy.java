package com.netcracker.cloud.framework.contexts.strategies;

import com.netcracker.cloud.context.propagation.core.Strategy;
import com.netcracker.cloud.context.propagation.core.contextdata.IncomingContextData;
import com.netcracker.cloud.context.propagation.core.supports.strategies.AbstractStrategy;
import org.jetbrains.annotations.Nullable;
import com.netcracker.cloud.framework.contexts.tenant.TenantContextObject;
import com.netcracker.cloud.framework.contexts.tenant.TenantNotFoundException;
import org.slf4j.MDC;

import java.util.function.Supplier;

public abstract class AbstractTenantStrategy extends AbstractStrategy<TenantContextObject> {

    protected abstract Strategy<TenantContextObject> getStrategy();
    private static final IncomingContextData nullContextData = null;
    public static final TenantContextObject DEFAULT_VALUE = new TenantContextObject(nullContextData);
    public static final String TENANT_ID = "tenantId";


    @Override
    protected Supplier<TenantContextObject> defaultObjectSupplier() {
        return () -> DEFAULT_VALUE;
    }

    @Override
    public void clear() {
        super.clear();
        MDC.remove(TENANT_ID);
    }

    @Override
    public void set(TenantContextObject value) {
        super.set(value);
        if (isValid(value)) {
            MDC.put(TENANT_ID, value.getTenant());
        } else {
            MDC.remove(TENANT_ID);
        }
    }

    @Override
    public TenantContextObject get() {
        TenantContextObject tenant = super.get();
        if (!isValid(tenant)) {
            throw new TenantNotFoundException("Tenant is not set");
        }
        return tenant;
    }

    @Override
    public boolean isValid(@Nullable TenantContextObject value) {
        return value != null && value.getTenant() != null && !value.getTenant().isEmpty();
    }
}

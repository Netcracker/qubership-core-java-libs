package com.netcracker.cloud.framework.contexts.tenant;

import org.jetbrains.annotations.Nullable;
import com.netcracker.cloud.context.propagation.core.RegisterProvider;
import com.netcracker.cloud.context.propagation.core.Strategy;
import com.netcracker.cloud.context.propagation.core.contextdata.IncomingContextData;

@RegisterProvider
public class DefaultTenantProvider extends BaseTenantProvider {
    private final Strategy<TenantContextObject> tenantContextStrategy = new DefaultTenantStrategy(() -> provide(null));

    @Override
    public Strategy<TenantContextObject> strategy() {
        return tenantContextStrategy;
    }

    @Override
    public int providerOrder() {
        return 100;
    }

    @Override
    public TenantContextObject provide(@Nullable IncomingContextData incomingContextData) {
        return new TenantContextObject(incomingContextData);
    }

    @Override
    public TenantContextObject provideFromSerializableData(IncomingContextData incomingContextData) {
        return null;
    }
}

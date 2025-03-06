package org.qubership.cloud.framework.contexts.tenant;

import org.qubership.cloud.context.propagation.core.ContextProvider;
import org.qubership.cloud.context.propagation.core.RegisterProvider;
import org.qubership.cloud.context.propagation.core.Strategy;
import org.qubership.cloud.context.propagation.core.contextdata.IncomingContextData;
import org.jetbrains.annotations.Nullable;


@RegisterProvider
public class TenantProvider implements ContextProvider<TenantContextObject> {
    public static final int DEFAULT_LEVEL = 0;
    public static final int TENANT_LEVEL = DEFAULT_LEVEL - 1;

    public static final String TENANT_CONTEXT_NAME = "tenant";
    private final Strategy<TenantContextObject> tenantContextStrategy = new TenantStrategy(() -> provide(null));

    @Override
    public Strategy<TenantContextObject> strategy() {
        return tenantContextStrategy;
    }

    @Override
    public int initLevel() {
        return TENANT_LEVEL;
    }

    @Override
    public int providerOrder() {
        return 100;
    }

    @Override
    public String contextName() {
        return TENANT_CONTEXT_NAME;
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

package org.qubership.cloud.framework.contexts.tenant;

import org.qubership.cloud.context.propagation.core.ContextProvider;

public abstract class BaseTenantProvider implements ContextProvider<TenantContextObject> {
    public static final int DEFAULT_LEVEL = 0;
    public static final int TENANT_LEVEL = DEFAULT_LEVEL - 1;
    public static final String TENANT_CONTEXT_NAME = "tenant";

    @Override
    public int initLevel() {
        return TENANT_LEVEL;
    }

    @Override
    public String contextName() {
        return TENANT_CONTEXT_NAME;
    }
}

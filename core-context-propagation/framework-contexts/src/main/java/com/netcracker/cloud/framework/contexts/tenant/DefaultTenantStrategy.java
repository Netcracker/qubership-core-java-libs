package com.netcracker.cloud.framework.contexts.tenant;

import com.netcracker.cloud.framework.contexts.strategies.AbstractTenantStrategy;
import com.netcracker.cloud.context.propagation.core.Strategy;
import com.netcracker.cloud.context.propagation.core.supports.strategies.DefaultStrategies;
import java.util.function.Supplier;

public class DefaultTenantStrategy extends AbstractTenantStrategy {

    private final Strategy<TenantContextObject> strategy;

    public DefaultTenantStrategy(Supplier<TenantContextObject> defaultContextObject) {
        strategy = DefaultStrategies.threadLocalWithInheritanceDefaultStrategy(defaultContextObject);
    }

    @Override
    public Strategy<TenantContextObject> getStrategy() {
        return strategy;
    }
}

package org.qubership.cloud.context.propagation.core.strategies;

import org.qubership.cloud.context.propagation.core.Strategy;
import org.qubership.cloud.context.propagation.core.contextdata.IncomingContextData;
import org.qubership.cloud.context.propagation.core.providers.xversion.XVersionContextObject;
import org.qubership.cloud.context.propagation.core.supports.strategies.DefaultStrategies;
import org.qubership.cloud.context.propagation.core.supports.strategies.ThreadLocalDefaultStrategy;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.function.Supplier;


class StrategiesBehaviourTest {
    @Test
    void testThreadLocalStrategy_ReturnsNullAfterNullSet() {
        Strategy<XVersionContextObject> strategy = getThreadLocalStrategy();
        strategy.set(null);

        Assertions.assertNull(strategy.get());
    }

    @Test
    void testThreadLocalStrategy_ReturnsDefaultAfterClear() {
        Strategy<XVersionContextObject> strategy = getThreadLocalStrategy();
        XVersionContextObject xVersionObj = new XVersionContextObject("test-x-version");
        strategy.set(xVersionObj);

        Assertions.assertEquals(xVersionObj, strategy.get());

        strategy.clear();

        Assertions.assertEquals(defaultContextObjectSupplier.get().getxVersion(), strategy.get().getxVersion());
        Assertions.assertEquals(strategy.get().getDefault(), strategy.get().getxVersion());
    }

    @Test
    void testRestEasyStrategy_ReturnsNullAfterNullSet() {
        Strategy<XVersionContextObject> strategy = getRestEasyStrategy();
        strategy.set(null);

        Assertions.assertNull(strategy.get());
    }

    @Test
    void testRestEasyStrategy_ReturnsDefaultAfterClear() {
        Strategy<XVersionContextObject> strategy = getRestEasyStrategy();
        XVersionContextObject xVersionObj = new XVersionContextObject("test-x-version");
        strategy.set(xVersionObj);

        Assertions.assertEquals(xVersionObj, strategy.get());

        strategy.clear();

        Assertions.assertEquals(defaultContextObjectSupplier.get().getxVersion(), strategy.get().getxVersion());
        Assertions.assertEquals(strategy.get().getDefault(), strategy.get().getxVersion());
    }

    @Test
    void testThreadLocalStrategyBehaviour_Equals_RestEasyStrategyBehaviour() {
        Strategy<XVersionContextObject> threadLocalStrategy = getThreadLocalStrategy();
        Strategy<XVersionContextObject> restEasyStrategy = getRestEasyStrategy();

        Assertions.assertEquals(threadLocalStrategy.get().getxVersion(), restEasyStrategy.get().getxVersion());
        Assertions.assertEquals(
                threadLocalStrategy.getSafe().map(XVersionContextObject::getxVersion),
                restEasyStrategy.getSafe().map(XVersionContextObject::getxVersion)
        );

        threadLocalStrategy.clear();
        restEasyStrategy.clear();

        Assertions.assertEquals(threadLocalStrategy.get().getxVersion(), restEasyStrategy.get().getxVersion());
        Assertions.assertEquals(
                threadLocalStrategy.getSafe().map(XVersionContextObject::getxVersion),
                restEasyStrategy.getSafe().map(XVersionContextObject::getxVersion)
        );

        threadLocalStrategy.set(null);
        restEasyStrategy.set(null);

        Assertions.assertEquals(threadLocalStrategy.get(), restEasyStrategy.get());
        Assertions.assertEquals(threadLocalStrategy.getSafe(), restEasyStrategy.getSafe());

        threadLocalStrategy.set(defaultContextObjectSupplier.get());
        restEasyStrategy.set(defaultContextObjectSupplier.get());

        Assertions.assertEquals(threadLocalStrategy.get().getxVersion(), restEasyStrategy.get().getxVersion());
        Assertions.assertEquals(
                threadLocalStrategy.getSafe().map(XVersionContextObject::getxVersion),
                restEasyStrategy.getSafe().map(XVersionContextObject::getxVersion)
        );
    }

    private Strategy<XVersionContextObject> getRestEasyStrategy() {
        return DefaultStrategies.restEasyDefaultStrategy(XVersionContextObject.class, defaultContextObjectSupplier);
    }

    private static Supplier<XVersionContextObject> defaultContextObjectSupplier = () -> new XVersionContextObject((IncomingContextData) null);

    private Strategy<XVersionContextObject> getThreadLocalStrategy() {
        return new ThreadLocalDefaultStrategy<>(defaultContextObjectSupplier);
    }
}

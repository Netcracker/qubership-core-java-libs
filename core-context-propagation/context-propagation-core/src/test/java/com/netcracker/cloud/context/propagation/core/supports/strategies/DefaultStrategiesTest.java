package com.netcracker.cloud.context.propagation.core.supports.strategies;

import org.qubership.cloud.context.propagation.core.contexts.common.RequestContextObject;
import org.qubership.cloud.context.propagation.core.contexts.common.RequestProvider;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class DefaultStrategiesTest {

    @Test
    void threadLocalWithInheritanceDefaultStrategy(){
        ThreadLocalWithInheritanceDefaultStrategy<RequestContextObject> testStrategy = DefaultStrategies.threadLocalWithInheritanceDefaultStrategy();
        ThreadLocalWithInheritanceDefaultStrategy<RequestContextObject> strategy = new ThreadLocalWithInheritanceDefaultStrategy<>();
        Assertions.assertEquals(testStrategy.getClass(), strategy.getClass());
    }

    @Test
    void threadLocalWithInheritanceDefaultStrategyWithSupplier(){
        RequestProvider requestProvider = new RequestProvider();
        ThreadLocalWithInheritanceDefaultStrategy<RequestContextObject> testStrategy = DefaultStrategies.threadLocalWithInheritanceDefaultStrategy(() -> requestProvider.provide(null));
        ThreadLocalWithInheritanceDefaultStrategy<RequestContextObject> strategy = new ThreadLocalWithInheritanceDefaultStrategy<>(() -> requestProvider.provide(null));
        Assertions.assertEquals(testStrategy.getClass(), strategy.getClass());
    }

    @Test
    void threadLocalDefaultStrategy(){
        ThreadLocalDefaultStrategy<RequestContextObject> testStrategy = DefaultStrategies.threadLocalDefaultStrategy();
        ThreadLocalDefaultStrategy<RequestContextObject> strategy = new ThreadLocalDefaultStrategy<>();
        Assertions.assertEquals(testStrategy.getClass(), strategy.getClass());
    }

    @Test
    void threadLocalDefaultStrategyWithSupplier(){
        RequestProvider requestProvider = new RequestProvider();
        ThreadLocalDefaultStrategy<RequestContextObject> testStrategy = DefaultStrategies.threadLocalDefaultStrategy(() -> requestProvider.provide(null));
        ThreadLocalDefaultStrategy<RequestContextObject> strategy = new ThreadLocalDefaultStrategy<>(() -> requestProvider.provide(null));
        Assertions.assertEquals(testStrategy.getClass(), strategy.getClass());
    }

    @Test
    void restEasyDefaultStrategy(){
        RequestProvider requestProvider = new RequestProvider();
        RestEasyDefaultStrategy<RequestContextObject> testStrategy = DefaultStrategies.restEasyDefaultStrategy(RequestContextObject.class, () -> requestProvider.provide(null));
        RestEasyDefaultStrategy<RequestContextObject> strategy = new RestEasyDefaultStrategy<>(RequestContextObject.class, () -> requestProvider.provide(null));
        Assertions.assertEquals(testStrategy.getClass(), strategy.getClass());
    }

}

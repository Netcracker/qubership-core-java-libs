package org.qubership.cloud.context.propagation.core.executors;


import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.concurrent.ExecutorService;

public class ContextAwareExecutorServiceApiTest {

    @Test
    public void constructorWithExecutorServiceOnly() throws NoSuchMethodException {
        Class<ContextAwareExecutorService> contextAwareExecutorServiceClass = ContextAwareExecutorService.class;
        Constructor<ContextAwareExecutorService> constructor = contextAwareExecutorServiceClass.getConstructor(ExecutorService.class);
        Assert.assertNotNull(constructor);
    }

    @Test
    public void constructorWithExecutorServiceAndMap() throws NoSuchMethodException {
        Class<ContextAwareExecutorService> contextAwareExecutorServiceClass = ContextAwareExecutorService.class;
        Constructor<ContextAwareExecutorService> constructorWithExecutorService = contextAwareExecutorServiceClass.getConstructor(ExecutorService.class, Map.class);
        Assert.assertNotNull(constructorWithExecutorService);
    }
}

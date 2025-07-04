package org.qubership.cloud.context.propagation.core.executors;



import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class ContextAwareExecutorServiceApiTest {

    @Test
    void constructorWithExecutorServiceOnly() throws NoSuchMethodException {
        Class<ContextAwareExecutorService> contextAwareExecutorServiceClass = ContextAwareExecutorService.class;
        Constructor<ContextAwareExecutorService> constructor = contextAwareExecutorServiceClass.getConstructor(ExecutorService.class);
        assertNotNull(constructor);
    }

    @Test
    void constructorWithExecutorServiceAndMap() throws NoSuchMethodException {
        Class<ContextAwareExecutorService> contextAwareExecutorServiceClass = ContextAwareExecutorService.class;
        Constructor<ContextAwareExecutorService> constructorWithExecutorService = contextAwareExecutorServiceClass.getConstructor(ExecutorService.class, Map.class);
        assertNotNull(constructorWithExecutorService);
    }
}

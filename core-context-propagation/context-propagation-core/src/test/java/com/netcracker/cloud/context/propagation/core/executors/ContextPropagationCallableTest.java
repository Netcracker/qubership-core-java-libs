package com.netcracker.cloud.context.propagation.core.executors;

import org.qubership.cloud.context.propagation.core.ContextManager;
import org.qubership.cloud.context.propagation.core.providers.xversion.XVersionContextObject;
import org.qubership.cloud.context.propagation.core.providers.xversion.XVersionProvider;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ContextPropagationCallableTest {

    @Test
    void checkCallablePropagation() throws ExecutionException, InterruptedException {
        XVersionContextObject v2 = new XVersionContextObject("v2");
        ContextManager.set(XVersionProvider.CONTEXT_NAME, v2);

        assertEquals("v2", ContextManager.<XVersionContextObject>get(XVersionProvider.CONTEXT_NAME).getxVersion());

        Map<String, Object> contextSnapshot = ContextManager.createContextSnapshot();
        ContextPropagationCallable contextPropagationCallable = new ContextPropagationCallable(contextSnapshot, () -> {
            assertEquals("v2", ContextManager.<XVersionContextObject>get(XVersionProvider.CONTEXT_NAME).getxVersion());

            XVersionContextObject v3 = new XVersionContextObject("v3");
            ContextManager.set(XVersionProvider.CONTEXT_NAME, v3);

            assertEquals("v3", ContextManager.<XVersionContextObject>get(XVersionProvider.CONTEXT_NAME).getxVersion());

            return null;
        });

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.submit(contextPropagationCallable).get();
        assertEquals("v2", ContextManager.<XVersionContextObject>get(XVersionProvider.CONTEXT_NAME).getxVersion());
    }

}

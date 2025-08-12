package com.netcracker.cloud.context.propagation.core.executors;

import com.netcracker.cloud.context.propagation.core.ContextManager;
import com.netcracker.cloud.context.propagation.core.providers.xversion.XVersionContextObject;
import com.netcracker.cloud.context.propagation.core.providers.xversion.XVersionProvider;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ContextPropagationSupplierTest {

    @Test
    void checkContextPropagation() throws ExecutionException, InterruptedException {
        XVersionContextObject xVersionContextObject = new XVersionContextObject("v1");
        ContextManager.set(XVersionProvider.CONTEXT_NAME, xVersionContextObject);

        assertEquals("v1", ContextManager.<XVersionContextObject>get(XVersionProvider.CONTEXT_NAME).getxVersion());
        ExecutorService executor = Executors.newSingleThreadExecutor();
        CompletableFuture completableFuture = CompletableFuture.supplyAsync(getContextAwarePropagationCallable("v1"), executor);
        assertEquals("v1", ContextManager.<XVersionContextObject>get(XVersionProvider.CONTEXT_NAME).getxVersion());
        completableFuture.get();

        XVersionContextObject v3 = new XVersionContextObject("v3");
        ContextManager.set(XVersionProvider.CONTEXT_NAME, v3);

        completableFuture = CompletableFuture.supplyAsync(getContextAwarePropagationCallable("v3"), executor);


        completableFuture.get();
    }

    @NotNull
    private ContextPropagationSupplier getContextAwarePropagationCallable(String expectedVersion) {
        return new ContextPropagationSupplier(ContextManager.createContextSnapshot(), () -> {
            assertEquals(expectedVersion, ContextManager.<XVersionContextObject>get(XVersionProvider.CONTEXT_NAME).getxVersion());

            XVersionContextObject v2 = new XVersionContextObject("v2");
            ContextManager.set(XVersionProvider.CONTEXT_NAME, v2);

            assertEquals("v2", ContextManager.<XVersionContextObject>get(XVersionProvider.CONTEXT_NAME).getxVersion());
            return null;
        });
    }
}

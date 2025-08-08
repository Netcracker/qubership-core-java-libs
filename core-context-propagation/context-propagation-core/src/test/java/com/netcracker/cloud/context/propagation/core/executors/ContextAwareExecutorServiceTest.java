package com.netcracker.cloud.context.propagation.core.executors;

import org.qubership.cloud.context.propagation.core.ContextManager;
import org.qubership.cloud.context.propagation.core.contextdata.IncomingContextData;
import org.qubership.cloud.context.propagation.core.providers.xversion.XVersionContextObject;
import org.qubership.cloud.context.propagation.core.providers.xversion.XVersionProvider;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

class ContextAwareExecutorServiceTest {

    @Test
    void executeWthCurrentContext() {
        // init current context
        XVersionContextObject xVersionContextObject = new XVersionContextObject((IncomingContextData) null);
        xVersionContextObject.setxVersion("v1");
        ContextManager.set(XVersionProvider.CONTEXT_NAME, xVersionContextObject);

        assertEquals("v1", ((XVersionContextObject) ContextManager.get(XVersionProvider.CONTEXT_NAME)).getxVersion());

        // create contextAwareExecutorService
        ExecutorService executorService = Executors.newCachedThreadPool();
        ContextAwareExecutorService contextAwareExecutorService = new ContextAwareExecutorService(executorService);
        try {
            contextAwareExecutorService.submit((Callable<Void>) () -> {
                // check current context is propagated and value exists
                assertNotNull(ContextManager.get(XVersionProvider.CONTEXT_NAME));
                assertEquals("v1", ((XVersionContextObject) ContextManager.get(XVersionProvider.CONTEXT_NAME)).getxVersion());

                // change context in task's thread
                XVersionContextObject newXVersionContextObject = new XVersionContextObject();
                newXVersionContextObject.setxVersion("v2");
                ContextManager.set(XVersionProvider.CONTEXT_NAME, newXVersionContextObject);

                return null;
            }).get();
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }

        // check context value outside of executorService remains the same
        assertEquals("v1", ((XVersionContextObject) ContextManager.get(XVersionProvider.CONTEXT_NAME)).getxVersion());
    }

    @Test
    void executeWthContextSnapshot_ContextAwareExecutorService() {
        // init current context
        XVersionContextObject xVersionContextObject = new XVersionContextObject((IncomingContextData) null);
        xVersionContextObject.setxVersion("v1");
        ContextManager.set(XVersionProvider.CONTEXT_NAME, xVersionContextObject);

        // create contextSnapshot
        Map<String, Object> contextSnapshot = ContextManager.createContextSnapshot();

        // change current context
        XVersionContextObject xVersionContextObjectNew = new XVersionContextObject((IncomingContextData) null);
        xVersionContextObjectNew.setxVersion("v2");
        ContextManager.set(XVersionProvider.CONTEXT_NAME, xVersionContextObjectNew);

        // create contextAwareExecutorService
        ExecutorService executorService = Executors.newCachedThreadPool();
        ContextAwareExecutorService contextAwareExecutorService = new ContextAwareExecutorService(executorService, contextSnapshot);
        try {
            contextAwareExecutorService.submit((Callable<Void>) () -> {

                // context according to contextSnapshot
                assertEquals("v1", ((XVersionContextObject) ContextManager.get(XVersionProvider.CONTEXT_NAME)).getxVersion());
                return null;
            }).get();
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }

        // check context value outside of executorService remains the same
        assertEquals("v2", ((XVersionContextObject) ContextManager.get(XVersionProvider.CONTEXT_NAME)).getxVersion());
    }

    @Test
    void executeWthContextSnapshot_ContextAwareExecutor() {
        // init current context
        XVersionContextObject xVersionContextObject = new XVersionContextObject((IncomingContextData) null);
        xVersionContextObject.setxVersion("v1");
        ContextManager.set(XVersionProvider.CONTEXT_NAME, xVersionContextObject);

        // create contextSnapshot
        Map<String, Object> contextSnapshot = ContextManager.createContextSnapshot();

        // change current context
        XVersionContextObject xVersionContextObjectNew = new XVersionContextObject((IncomingContextData) null);
        xVersionContextObjectNew.setxVersion("v2");
        ContextManager.set(XVersionProvider.CONTEXT_NAME, xVersionContextObjectNew);

        // create contextAwareExecutorService
        Executor executor = new ThreadPerTaskExecutor();
        ContextAwareExecutor contextAwareExecutor = new ContextAwareExecutor(executor, contextSnapshot);
        try {
            CompletableFuture<String> cpf = new CompletableFuture<>();
            contextAwareExecutor.execute(() -> {
                // context according to contextSnapshot
                cpf.complete(((XVersionContextObject) ContextManager.get(XVersionProvider.CONTEXT_NAME)).getxVersion());
            });
            assertEquals("v1", cpf.get());
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }

        // check context value outside of executorService remains the same
        assertEquals("v2", ((XVersionContextObject) ContextManager.get(XVersionProvider.CONTEXT_NAME)).getxVersion());
    }

    class ThreadPerTaskExecutor implements Executor {
        public void execute(Runnable r) {
            new Thread(r).start();
        }
    }
}

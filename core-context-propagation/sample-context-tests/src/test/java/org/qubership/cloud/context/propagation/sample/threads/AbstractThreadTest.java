package org.qubership.cloud.context.propagation.sample.threads;

import org.qubership.cloud.context.propagation.core.ContextManager;
import org.qubership.cloud.context.propagation.core.executors.ContextAwareExecutorService;
import org.junit.After;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AbstractThreadTest {
    final ExecutorService simpleExecutor = new ContextAwareExecutorService(Executors.newFixedThreadPool(2));
    @After
    public void tearDown() {
        ContextManager.clearAll();
    }
}

package com.netcracker.cloud.context.propagation.sample.threads;

import org.junit.jupiter.api.AfterEach;
import org.qubership.cloud.context.propagation.core.ContextManager;
import org.qubership.cloud.context.propagation.core.executors.ContextAwareExecutorService;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AbstractThreadTest {
    final ExecutorService simpleExecutor = new ContextAwareExecutorService(Executors.newFixedThreadPool(2));
    @AfterEach
    void tearDown() {
        ContextManager.clearAll();
    }
}

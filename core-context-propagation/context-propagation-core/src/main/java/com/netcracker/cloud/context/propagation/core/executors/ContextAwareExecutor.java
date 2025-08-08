package com.netcracker.cloud.context.propagation.core.executors;

import org.qubership.cloud.context.propagation.core.ContextManager;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class ContextAwareExecutor implements Executor {

    protected Executor delegate;
    protected Map<String, Object> contextSnapshot;

    public ContextAwareExecutor(Executor executor) {
        this.delegate = executor;
    }

    public ContextAwareExecutor(Executor delegate, Map<String, Object> contextSnapshot) {
        this.delegate = delegate;
        this.contextSnapshot = contextSnapshot;
    }

    @Override
    public void execute(@NotNull Runnable command) {
        delegate.execute(wrap(command));
    }

    protected Runnable wrap(final Runnable runnable) {
        if (runnable != null) {
            final Callable<?> callable = Executors.callable(runnable);
            final Callable<?> wrapped = wrap(callable);
            return new RunnableAdapter(wrapped);
        }
        return runnable;
    }

    protected  <V> Callable<V> wrap(final Callable<V> delegate) {
        return new ContextPropagationCallable<>(
                contextSnapshot == null ? ContextManager.createContextSnapshot() : contextSnapshot,
                delegate
        );
    }
}

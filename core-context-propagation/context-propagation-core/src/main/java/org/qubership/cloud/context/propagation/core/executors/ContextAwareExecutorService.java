package org.qubership.cloud.context.propagation.core.executors;

import org.qubership.cloud.context.propagation.core.ContextManager;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * The class allows executing {@link Callable} or {@link Runnable} tasks within active or snapshot context.
 * Parent thread will not be affected.
 */
public class ContextAwareExecutorService extends ContextAwareExecutor implements ExecutorService {

    /**
     * This constructor builds a ContextAwareExecutorService that will execute all tasks
     * within the passed contextSnapshot.
     *
     * @param delegate        this executorService will do all work under submitted tasks
     * @param contextSnapshot it's a snapshot of context state (number of contexts, their value). This context state
     *                        will be applied to each task. ContextSnapshot can be taken by the method:
     *                        {@link ContextManager#createContextSnapshot() ContextManager.createContextSnapshot()}
     */
    public ContextAwareExecutorService(ExecutorService delegate, Map<String, Object> contextSnapshot) {
        super(delegate, contextSnapshot);
        this.contextSnapshot = contextSnapshot;
    }

    /**
     * This constructor builds a ContextAwareExecutorService that will execute all tasks
     * within a context which has state at the time of task submission. See method:
     * {@link #wrap(Callable)}
     *
     * @param executorService this executorService will do all work under submitted tasks
     */
    public ContextAwareExecutorService(ExecutorService executorService) {
        super(executorService);
    }


    public void shutdown() {
        ((ExecutorService)delegate).shutdown();
    }

    public List<Runnable> shutdownNow() {
        return ((ExecutorService)delegate).shutdownNow();
    }

    public boolean isShutdown() {
        return ((ExecutorService)delegate).isShutdown();
    }

    public boolean isTerminated() {
        return ((ExecutorService)delegate).isTerminated();
    }

    public boolean awaitTermination(long timeout, @NotNull TimeUnit unit) throws InterruptedException {
        return ((ExecutorService)delegate).awaitTermination(timeout, unit);
    }

    public <T> Future<T> submit(@NotNull Callable<T> task) {
        return ((ExecutorService)delegate).submit(wrap(task));
    }

    public <T> Future<T> submit(@NotNull Runnable task, T result) {
        return ((ExecutorService)delegate).submit(wrap(task), result);
    }

    public Future<?> submit(@NotNull Runnable task) {
        return ((ExecutorService)delegate).submit(wrap(task));
    }

    private <T> Collection<? extends Callable<T>> wrapTasks(Collection<? extends Callable<T>> tasks) {
        Collection<? extends Callable<T>> wrappedTasks = tasks;
        if (tasks != null && !tasks.isEmpty()) {
            final List<Callable<T>> copy = new ArrayList<Callable<T>>(tasks.size());
            for (Callable<T> task : tasks) {
                final Callable<T> wrapped = wrap(task);
                copy.add(wrapped);
            }
            wrappedTasks = copy;
        }
        return wrappedTasks;
    }

    public <T> List<Future<T>> invokeAll(@NotNull Collection<? extends Callable<T>> tasks) throws InterruptedException {
        return ((ExecutorService)delegate).invokeAll(wrapTasks(tasks));
    }

    public <T> List<Future<T>> invokeAll(@NotNull Collection<? extends Callable<T>> tasks, long timeout, @NotNull TimeUnit unit) throws InterruptedException {
        return ((ExecutorService)delegate).invokeAll(wrapTasks(tasks), timeout, unit);
    }

    public <T> T invokeAny(@NotNull Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        return ((ExecutorService)delegate).invokeAny(wrapTasks(tasks));
    }

    public <T> T invokeAny(@NotNull Collection<? extends Callable<T>> tasks, long timeout, @NotNull TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return ((ExecutorService)delegate).invokeAny(wrapTasks(tasks), timeout, unit);
    }
}

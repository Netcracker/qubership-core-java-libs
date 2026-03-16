package com.netcracker.core.scheduler.helpers;

import com.github.kagkarlsson.scheduler.task.Execution;
import com.netcracker.core.scheduler.po.FutureKey;
import com.netcracker.core.scheduler.po.ProcessOrchestrator;
import com.netcracker.core.scheduler.po.model.pojo.ProcessInstanceImpl;
import com.netcracker.core.scheduler.po.model.pojo.TaskInstanceImpl;
import com.netcracker.core.scheduler.po.runnable.TaskExecutionWrapper;
import com.netcracker.core.scheduler.po.runnable.TerminateRunnable;
import com.netcracker.core.scheduler.po.task.TaskState;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.*;

public class TaskExecutorService implements ExecutorService {
    private static final Logger log = LoggerFactory.getLogger(TaskExecutorService.class);
    MethodHandle getExecutor;
    MethodHandle getExecution;

    private final ExecutorService delegate;
    private final ExecutorService woreService;
    private final Map<FutureKey, Future<Boolean>> tasks;

    public TaskExecutorService(Integer threads) {
        delegate = Executors.newFixedThreadPool(threads);
        tasks = new ConcurrentHashMap<>();
        woreService = Executors.newVirtualThreadPerTaskExecutor();
    }

    @Override
    public void shutdown() {
        delegate.shutdown();
    }

    @Override
    @NotNull
    public List<Runnable> shutdownNow() {
        return delegate.shutdownNow();
    }

    @Override
    public boolean isShutdown() {
        return delegate.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return delegate.isTerminated();
    }

    @Override
    public boolean awaitTermination(long timeout, @NotNull TimeUnit unit) throws InterruptedException {
        return delegate.awaitTermination(timeout, unit);
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        return delegate.submit(task);
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        return delegate.submit(task, result);
    }

    @Override
    public Future<?> submit(Runnable task) {
        return delegate.submit(task);
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        return delegate.invokeAll(tasks);
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
        return delegate.invokeAll(tasks, timeout, unit);
    }

    @Override
    public <T> T invokeAny(@NotNull Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        return delegate.invokeAny(tasks);
    }

    @Override
    public <T> T invokeAny(@NotNull Collection<? extends Callable<T>> tasks, long timeout, @NotNull TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return delegate.invokeAny(tasks, timeout, unit);
    }

    @Override
    public void execute(@NotNull Runnable command) {
        final Execution execution = getId(command);
        if (execution != null) {
            String taskId = execution.taskInstance.getId();
            final TaskInstanceImpl taskInstance = ProcessOrchestrator.getInstance().getTaskInstanceRepository().getTaskInstance(taskId);
            final FutureKey fk = new FutureKey(taskId);
            final FutureKey fk2 = new FutureKey("Sync"+taskId);
            final TaskExecutionWrapper wrapper = new TaskExecutionWrapper(command, () -> {
                tasks.remove(fk);
                Future<?> f = tasks.remove(fk2);
                if (f != null) f.cancel(true);
            });
            long timeout = taskInstance == null ? 0L : taskInstance.getTimeout();
            final Future<Boolean> future = delegate.submit(wrapper);
            tasks.put(fk, future);
            if (timeout != 0L) {
                tasks.put(fk2, delegate.submit(() -> {
                    try {
                        future.get(timeout, TimeUnit.SECONDS);
                    } catch (InterruptedException | ExecutionException | TimeoutException e) {
                        log.info("Task {} execution was interrupted by timeout", taskId);
                        future.cancel(true);
                        ProcessInstanceImpl processInstance = ProcessOrchestrator.getInstance().getProcessInstance(taskInstance.getProcessID());
                        processInstance.setState(TaskState.FAILED);
                        processInstance.save();
                        TaskInstanceImpl task = ProcessOrchestrator.getInstance().getTaskInstanceRepository().getTaskInstance(taskId);
                        task.setState(TaskState.FAILED);
                        task.save();
                    }

                    return Boolean.TRUE;
                }));
            }


        }
    }


    @Nullable
    private Execution getId(Runnable task) {
        try {


            if (getExecutor == null) {
                Field arg2F = task.getClass().getDeclaredField("arg$2");
                arg2F.setAccessible(true);
                getExecutor = MethodHandles.lookup().unreflectGetter(arg2F);
            }

            Object arg2 = getExecutor.invoke(task);
            if (getExecution == null) {
                Field arg2F2 = arg2.getClass().getDeclaredField("arg$2");
                arg2F2.setAccessible(true);
                getExecution = MethodHandles.lookup().unreflectGetter(arg2F2);
            }

            return (Execution) getExecution.invoke(arg2);
        } catch (Throwable e) {
            return null;
        }
    }

    public Future<?> terminate(String key) {
        List<Future<Boolean>> fs = tasks
                .entrySet()
                .stream()
                .filter(e -> e.getKey().equals(key))
                .map(f -> woreService.submit(new TerminateRunnable(f.getValue(), key)))
                .toList();
        return woreService.submit(() -> {
                    if (!fs.isEmpty())
                        fs.forEach(f -> {
                            try {
                                f.get();
                            } catch (InterruptedException | ExecutionException e) {
                                throw new RuntimeException(e);
                            }
                        });
                }
        );
    }
}

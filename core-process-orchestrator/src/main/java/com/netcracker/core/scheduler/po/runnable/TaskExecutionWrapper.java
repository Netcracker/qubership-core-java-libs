package com.netcracker.core.scheduler.po.runnable;

import java.util.concurrent.Callable;

public class TaskExecutionWrapper implements Callable<Boolean> {

    private final Runnable callback;
    private final Runnable task;

    public TaskExecutionWrapper(Runnable task, Runnable callback) {
        this.callback = callback;
        this.task = task;
    }

    @Override
    public Boolean call() throws Exception {
        try {
            task.run();
        } catch (Throwable e) {
            callback.run();
        } finally {
            callback.run();
        }
        return Boolean.TRUE;
    }
}

package com.netcracker.core.scheduler.po.runnable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;

public class TaskExecutionWrapper implements Callable<Boolean> {

    private static final Logger logger = LoggerFactory.getLogger(TaskExecutionWrapper.class);

    private final Runnable callback;
    private final Runnable task;

    public TaskExecutionWrapper(Runnable task, Runnable callback) {
        this.callback = callback;
        this.task = task;
    }

    @Override
    public Boolean call() throws Exception {
        // The callback must run exactly once: the previous catch-plus-finally
        // shape ran it twice on failure. Errors are logged, not rethrown — the
        // task's own failure handling is done by the scheduler's failure hooks.
        try {
            task.run();
        } catch (Exception e) {
            logger.error("Task execution failed", e);
        } finally {
            callback.run();
        }
        return Boolean.TRUE;
    }
}

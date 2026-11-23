package com.netcracker.core.scheduler.po.samples.tasks;

import com.github.kagkarlsson.scheduler.task.ExecutionContext;
import com.github.kagkarlsson.scheduler.task.TaskInstance;
import com.netcracker.core.scheduler.po.context.TaskExecutionContext;
import com.netcracker.core.scheduler.po.task.templates.AbstractProcessTask;
import lombok.SneakyThrows;

public class SyncTimeoutTask extends AbstractProcessTask {
    public SyncTimeoutTask() {
        super(SyncTimeoutTask.class.getName());
    }

    @SneakyThrows
    @Override
    public void executeInternal(TaskInstance<TaskExecutionContext> taskInstance, ExecutionContext executionContext) {
        Thread.sleep(5000);
    }
}

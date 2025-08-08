package com.netcracker.core.scheduler.po.samples.tasks;

import com.github.kagkarlsson.scheduler.task.ExecutionContext;
import com.github.kagkarlsson.scheduler.task.TaskInstance;
import org.qubership.core.scheduler.po.context.TaskExecutionContext;
import org.qubership.core.scheduler.po.task.templates.AbstractProcessTask;

public class DataTask extends AbstractProcessTask {
    public DataTask() {
        super(DataTask.class.getName());
    }

    @Override
    public void executeInternal(TaskInstance<TaskExecutionContext> taskInstance, ExecutionContext executionContext) {
        if (!taskInstance.getData().getProcess().getContext().containsKey("MyData")) throw new RuntimeException();
    }
}

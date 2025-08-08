package com.netcracker.core.scheduler.po.samples.tasks;

import com.github.kagkarlsson.scheduler.task.ExecutionContext;
import com.github.kagkarlsson.scheduler.task.TaskInstance;
import org.qubership.core.scheduler.po.context.TaskExecutionContext;
import org.qubership.core.scheduler.po.task.templates.AbstractProcessTask;

public class DummyTask2 extends AbstractProcessTask {
    public DummyTask2() {
        super(DummyTask2.class.getName());
    }

    @Override
    public void executeInternal(TaskInstance<TaskExecutionContext> taskInstance, ExecutionContext executionContext) {
    }

}

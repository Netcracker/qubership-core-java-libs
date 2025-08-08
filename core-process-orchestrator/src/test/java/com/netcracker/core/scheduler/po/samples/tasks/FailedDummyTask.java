package com.netcracker.core.scheduler.po.samples.tasks;

import com.github.kagkarlsson.scheduler.task.ExecutionContext;
import com.github.kagkarlsson.scheduler.task.TaskInstance;
import org.qubership.core.scheduler.po.DataContext;
import org.qubership.core.scheduler.po.context.TaskExecutionContext;
import org.qubership.core.scheduler.po.samples.TestFailureHandler;
import org.qubership.core.scheduler.po.task.templates.AbstractProcessTask;

public class FailedDummyTask extends AbstractProcessTask {
    public FailedDummyTask() {
        super(FailedDummyTask.class.getName(), new TestFailureHandler());
    }

    @Override
    public void executeInternal(TaskInstance<TaskExecutionContext> taskInstance, ExecutionContext executionContext) {
        taskInstance.getData().getProcess().getContext().apply((DataContext dataContext) -> dataContext.put("MyData2", "MyData"));
        throw new RuntimeException("error");
    }
}

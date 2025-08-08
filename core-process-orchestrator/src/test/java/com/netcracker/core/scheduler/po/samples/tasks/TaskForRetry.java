package com.netcracker.core.scheduler.po.samples.tasks;

import com.github.kagkarlsson.scheduler.task.ExecutionContext;
import com.github.kagkarlsson.scheduler.task.TaskInstance;
import org.qubership.core.scheduler.po.DataContext;
import org.qubership.core.scheduler.po.context.TaskExecutionContext;
import org.qubership.core.scheduler.po.task.templates.AbstractProcessTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TaskForRetry extends AbstractProcessTask {
    private static final Logger logger= LoggerFactory.getLogger(TaskForRetry.class);
    public TaskForRetry() {
        super(TaskForRetry.class.getName());
    }

    @Override
    public void executeInternal(TaskInstance<TaskExecutionContext> taskInstance, ExecutionContext executionContext) {

        DataContext context = taskInstance.getData().getProcess().getContext();
        if (!Boolean.FALSE.equals(context.get("isFail"))) {
            throw new RuntimeException("Retry Test");
        }
        logger.info("Retry success");

    }
}

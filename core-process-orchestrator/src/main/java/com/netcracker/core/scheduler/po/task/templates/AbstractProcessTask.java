package com.netcracker.core.scheduler.po.task.templates;

import com.github.kagkarlsson.scheduler.task.*;
import com.github.kagkarlsson.scheduler.task.helper.CustomTask;
import org.qubership.core.scheduler.po.context.TaskExecutionContext;
import org.qubership.core.scheduler.po.model.pojo.TaskInstanceImpl;
import org.qubership.core.scheduler.po.task.ProcessTaskFailureHandler;
import org.qubership.core.scheduler.po.task.TaskState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Calendar;
import java.util.function.Function;

import static org.qubership.core.scheduler.po.task.TaskState.TERMINATED;

public abstract class AbstractProcessTask extends CustomTask<TaskExecutionContext> {

    public static TaskInstance<TaskExecutionContext> getTaskInstance(String taskClass, String id, TaskExecutionContext context) {
        return new TaskInstance<>(taskClass, id, context);
    }

    private static final Logger logger = LoggerFactory.getLogger(AbstractProcessTask.class);

    protected AbstractProcessTask(String name) {
        super(
                name,
                TaskExecutionContext.class,
                null,
                Function.identity(),
                new ProcessTaskFailureHandler(),
                new DeadExecutionHandler.ReviveDeadExecution<>());
    }

    protected AbstractProcessTask(String name, FailureHandler<TaskExecutionContext> failureHandler) {
        super(
                name,
                TaskExecutionContext.class,
                null,
                Function.identity(),
                new ProcessTaskFailureHandler(failureHandler),
                new DeadExecutionHandler.ReviveDeadExecution<>());
    }

    public abstract void executeInternal(TaskInstance<TaskExecutionContext> taskInstance, ExecutionContext executionContext);

    @Override
    public CompletionHandler<TaskExecutionContext> execute(TaskInstance<TaskExecutionContext> taskInstance, ExecutionContext executionContext) {
        logger.info("Executing task {} with id {}", getTaskName(), taskInstance.getId());
        executeInternal(taskInstance, executionContext);
        TaskInstanceImpl task = taskInstance.getData().getTaskInstance();
        if(!TERMINATED.equals(task.getState())) {
            task.setEndTime(Calendar.getInstance().getTime());
            task.setState(TaskState.COMPLETED);
            task.save();
            logger.info("Task {} with id {} completed.", getTaskName(), taskInstance.getId());
        }
        return (executionComplete, executionOperations) -> executionOperations.remove();
    }

}

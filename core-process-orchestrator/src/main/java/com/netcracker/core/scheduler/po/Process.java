package com.netcracker.core.scheduler.po;

import com.github.kagkarlsson.scheduler.task.CompletionHandler;
import com.github.kagkarlsson.scheduler.task.DeadExecutionHandler;
import com.github.kagkarlsson.scheduler.task.ExecutionContext;
import com.github.kagkarlsson.scheduler.task.helper.CustomTask;
import org.qubership.core.scheduler.po.context.ProcessContext;
import org.qubership.core.scheduler.po.context.TaskExecutionContext;
import org.qubership.core.scheduler.po.model.pojo.ProcessInstanceImpl;
import org.qubership.core.scheduler.po.model.pojo.TaskInstanceImpl;
import org.qubership.core.scheduler.po.task.TaskState;
import org.qubership.core.scheduler.po.task.templates.AbstractProcessTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Calendar;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Process extends CustomTask<ProcessContext> {

    public static final String PO_TASK_NAME = "POProcess";
    private static final Logger logger = LoggerFactory.getLogger(Process.class);

    public Process() {
        super(
                PO_TASK_NAME,
                ProcessContext.class,
                null,
                Function.identity(),
                (executionComplete, executionOperations) -> logger.error("Process with ID={} failed ", executionComplete.getExecution().taskInstance.getId()),
                new DeadExecutionHandler.ReviveDeadExecution<>());
    }

    @Override
    public CompletionHandler<ProcessContext> execute(com.github.kagkarlsson.scheduler.task.TaskInstance<ProcessContext> taskInstance, ExecutionContext executionContext) {
        ProcessInstanceImpl processInstance = taskInstance.getData().getProcess();
        if (processInstance.getState() == TaskState.TERMINATED) {
            return (executionComplete, executionOperations) -> executionOperations.remove();
        }
        for (TaskInstanceImpl task : processInstance.getTasks().stream().filter(TaskInstanceImpl::isReady).collect(Collectors.toSet())) {
            task.setState(TaskState.IN_PROGRESS);
            task.setStartTime(Calendar.getInstance().getTime());
            task.save();
            executionContext.getSchedulerClient().schedule(AbstractProcessTask.getTaskInstance(task.getType(), task.getId(), new TaskExecutionContext(processInstance.getId(), task.getId())), Instant.now());
        }

        return (executionComplete, executionOperations) -> {
            // On Complete processing

            boolean completed = true;

            for (TaskInstanceImpl task : taskInstance.getData().getProcess().getTasks()) {
                if (task.getState() == TaskState.FAILED) {
                    logger.info("Process {} are failed", taskInstance.getId());
                    taskInstance.getData().getProcess().setState(TaskState.FAILED);
                    executionOperations.remove();
                    taskInstance.getData().getProcess().save();
                    return;
                }
                completed = completed && task.getState() == TaskState.COMPLETED;
            }
            if (!completed)
                executionOperations.reschedule(executionComplete, Instant.now().plusSeconds(2), taskInstance.getData());
            else {
                logger.info("Process {} are completed", taskInstance.getId());
                taskInstance.getData().getProcess().setEndTime(Calendar.getInstance().getTime());
                taskInstance.getData().getProcess().setState(TaskState.COMPLETED);
                taskInstance.getData().getProcess().save();
                executionOperations.remove();
            }
            taskInstance.getData().getProcess().save();
        };
    }

}

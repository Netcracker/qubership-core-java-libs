package org.qubership.core.scheduler.po.task;

import com.github.kagkarlsson.scheduler.task.ExecutionComplete;
import com.github.kagkarlsson.scheduler.task.ExecutionOperations;
import com.github.kagkarlsson.scheduler.task.FailureHandler;
import org.qubership.core.scheduler.po.context.TaskExecutionContext;
import org.qubership.core.scheduler.po.model.pojo.TaskInstanceImpl;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class ProcessTaskFailureHandler implements FailureHandler<TaskExecutionContext> {
    private static final Logger logger = LoggerFactory.getLogger(ProcessTaskFailureHandler.class);
    protected FailureHandler<TaskExecutionContext> delegate;

    public ProcessTaskFailureHandler() {
        delegate = null;
    }

    public ProcessTaskFailureHandler(FailureHandler<TaskExecutionContext> delegate) {
        this.delegate = delegate;
    }

    @SneakyThrows
    @Override
    public void onFailure(ExecutionComplete executionComplete, ExecutionOperations<TaskExecutionContext> executionOperations) {

        List<Throwable> list = new ArrayList<>();
        if (executionComplete.getCause().isPresent()) {
            Throwable ex = executionComplete.getCause().get();
            while (ex != null) {
                list.add(ex);
                ex = ex.getCause();
            }
        }
        if (list.stream().noneMatch(t -> t instanceof InterruptedException) && !Thread.currentThread().isInterrupted()) {
            TaskInstanceImpl task = ((TaskExecutionContext) executionComplete.getExecution().taskInstance.getData()).getTaskInstance();
            if (delegate != null) delegate.onFailure(executionComplete, executionOperations);
            task.setState(TaskState.FAILED);
            task.setEndTime(Calendar.getInstance().getTime());
            task.save();
            executionOperations.remove();
            logger.error("Task {} with ID:{} failed", executionComplete.getExecution().taskInstance.getTaskName(), executionComplete.getExecution().taskInstance.getId());
        }
        else {
            logger.warn("Going to remove terminated task {}",executionComplete.getExecution().taskInstance.getId());
            executionOperations.remove();
        }
    }


}

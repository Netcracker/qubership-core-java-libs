package org.qubership.core.scheduler.po.task.templates;

import com.github.kagkarlsson.scheduler.task.CompletionHandler;
import com.github.kagkarlsson.scheduler.task.ExecutionContext;
import com.github.kagkarlsson.scheduler.task.FailureHandler;
import com.github.kagkarlsson.scheduler.task.TaskInstance;
import org.qubership.core.scheduler.po.AsyncTimeoutException;
import org.qubership.core.scheduler.po.DataContext;
import org.qubership.core.scheduler.po.context.TaskExecutionContext;
import org.qubership.core.scheduler.po.model.pojo.ProcessInstanceImpl;
import org.qubership.core.scheduler.po.model.pojo.TaskInstanceImpl;
import org.qubership.core.scheduler.po.task.TaskState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Calendar;
import java.util.function.Consumer;
import java.util.function.Supplier;

public abstract class AsyncTaskWithPolling extends AbstractProcessTask {
    protected long polingInterval = 10;

    private static class InstantSuppler implements Consumer<Instant>, Supplier<Instant> {

        private Instant value;

        @Override
        public void accept(Instant instant) {
            value = instant;
        }

        @Override
        public Instant get() {
            return value;
        }
    }

    private final String synPartFlag = String.format("%s-syncPartFlag", this.getClass().getName());
    private static final Logger logger = LoggerFactory.getLogger(AsyncTaskWithPolling.class);

    protected AsyncTaskWithPolling(String name) {
        super(name);
    }

    @SuppressWarnings("unused")
    protected AsyncTaskWithPolling(String name, FailureHandler<TaskExecutionContext> failureHandler) {
        super(name, failureHandler);
    }

    @Override
    public CompletionHandler<TaskExecutionContext> execute(TaskInstance<TaskExecutionContext> taskInstance, ExecutionContext executionContext) {
        TaskInstanceImpl task = taskInstance.getData().getTaskInstance();
        DataContext taskContext = task.getContext();
        logger.info("Executing task {} with id {}", getTaskName(), taskInstance.getId());
        if (task.getAsyncTimeout() != 0 && Calendar.getInstance().getTime().getTime() - task.getStartTime().getTime() > task.getAsyncTimeout() * 1000) {
            logger.info("Task {} execution was interrupted by async timeout", task.getId());
            throw new AsyncTimeoutException();
        }
        final InstantSuppler when = new InstantSuppler();
        when.accept(Instant.now().plusSeconds(polingInterval));
        task.save();
        if (Boolean.FALSE.equals(taskContext.computeIfAbsent(synPartFlag, s -> Boolean.FALSE))) {
            executeSyncPart(taskInstance.getData());
            taskContext.put(synPartFlag, Boolean.TRUE);
            taskContext.save();
        } else {
            try {
                if (executePolling(taskInstance.getData())) {
                    onPollingSuccess(taskInstance.getData());
                    task.setEndTime(Calendar.getInstance().getTime());
                    task.setState(TaskState.COMPLETED);
                    task.save();
                    when.accept(null);
                }
            } catch (Exception e) {
                onPollingFailed(taskInstance.getData(), e);
                task.setState(TaskState.FAILED);
                ProcessInstanceImpl processInstance = taskInstance.getData().getProcess();
                processInstance.setState(TaskState.FAILED);
                processInstance.save();
                task.save();
                when.accept(null);
            }

        }
        return (executionComplete, executionOperations) -> {
            if (when.get() == null)
                executionOperations.remove();
            else
                executionOperations.reschedule(executionComplete, when.get(), taskInstance.getData());
        };
    }

    public abstract void executeSyncPart(TaskExecutionContext instance);

    public abstract boolean executePolling(TaskExecutionContext instance);

    public abstract void onPollingSuccess(TaskExecutionContext instance);

    public abstract void onPollingFailed(TaskExecutionContext instance, Exception exception);

    @Override
    //Not Used in Async Task
    public void executeInternal(TaskInstance<TaskExecutionContext> taskInstance, ExecutionContext executionContext) {
    }
}

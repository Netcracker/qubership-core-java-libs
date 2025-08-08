package com.netcracker.core.scheduler.po.samples.tasks;

import com.github.kagkarlsson.scheduler.task.ExecutionContext;
import com.github.kagkarlsson.scheduler.task.TaskInstance;
import com.netcracker.core.scheduler.po.ProcessOrchestrator;
import com.netcracker.core.scheduler.po.context.TaskExecutionContext;
import com.netcracker.core.scheduler.po.task.templates.AbstractProcessTask;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LongRunningTask extends AbstractProcessTask {
    private static final Logger log = LoggerFactory.getLogger(LongRunningTask.class);

    public LongRunningTask() {
        super(LongRunningTask.class.getName());
    }


    @SneakyThrows
    @Override
    public void executeInternal(TaskInstance<TaskExecutionContext> taskInstance, ExecutionContext executionContext) {
        String taskName = ProcessOrchestrator.getInstance().getTaskInstanceRepository().getTaskInstance(taskInstance.getId()).getName();
        for (int i = 0; i < 10; i++) {
            Thread.sleep(100);
            log.info("Ping {}", taskName);
        }
    }
}

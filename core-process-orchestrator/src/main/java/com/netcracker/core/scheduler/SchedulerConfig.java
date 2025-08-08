package com.netcracker.core.scheduler;

import com.github.kagkarlsson.scheduler.Scheduler;
import com.github.kagkarlsson.scheduler.task.Task;
import org.qubership.core.scheduler.helpers.SchedulerUtils;
import org.qubership.core.scheduler.po.Process;
import org.qubership.core.scheduler.po.ProcessOrchestrator;

import javax.sql.DataSource;
import java.lang.reflect.InvocationTargetException;
import java.time.Duration;
import java.util.List;

public class SchedulerConfig {
    private SchedulerConfig() {
    }

    public static Scheduler getScheduler(DataSource dataSource) throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        List<Task<?>> knownTasks = ProcessOrchestrator.getTasks();
        knownTasks.add(new Process());
        SchedulerUtils.scheduler = Scheduler
                .create(dataSource, knownTasks)
                .enableImmediateExecution() // will cause job scheduled to now() to run directly
                .pollingInterval(Duration.ofSeconds(10))
                .registerShutdownHook()
                .build();
        SchedulerUtils.scheduler.start();
        return SchedulerUtils.scheduler;
    }

}

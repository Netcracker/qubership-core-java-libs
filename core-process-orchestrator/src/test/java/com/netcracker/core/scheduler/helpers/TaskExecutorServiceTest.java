package com.netcracker.core.scheduler.helpers;

import com.netcracker.core.scheduler.po.ProcessOrchestrator;
import com.netcracker.core.scheduler.po.model.pojo.ProcessInstanceImpl;
import com.netcracker.core.scheduler.po.model.pojo.TaskInstanceImpl;
import com.netcracker.core.scheduler.po.samples.tasks.DummyTask2;
import com.netcracker.core.scheduler.po.task.TaskState;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

/**
 * Covers the sync-timeout watchdog. The completion callback cancels the
 * watchdog with an interrupt when the task finishes in time, so an interrupt
 * must leave the task and process state untouched; only a real timeout may
 * mark them FAILED.
 */
class TaskExecutorServiceTest {

    DataSource dataSource;
    ProcessOrchestrator orchestrator;
    TaskExecutorService service;

    @BeforeEach
    void setup() throws Exception {
        dataSource = SchedulerUtils.initDatabase();
        orchestrator = new ProcessOrchestrator(dataSource);
        service = new TaskExecutorService(2);
    }

    @AfterEach
    void teardown() throws Exception {
        service.shutdown();
        orchestrator.stop();
        ((HikariDataSource) dataSource).close();
    }

    private void seedTaskAndProcess(TaskState state) {
        TaskInstanceImpl task = new TaskInstanceImpl("task-1", "TestTask", DummyTask2.class.getName(), "proc-1");
        task.setState(state);
        orchestrator.getTaskInstanceRepository().putTaskInstance(task);

        ProcessInstanceImpl process = new ProcessInstanceImpl("Test Instance", "proc-1", "def-1");
        process.setState(state);
        orchestrator.getProcessInstanceRepository().putProcessInstance(process);
    }

    @Test
    void interruptedWatchdogLeavesACompletedTaskUntouched() throws Exception {
        seedTaskAndProcess(TaskState.COMPLETED);

        // The worker future is still pending when the interrupt arrives, exactly
        // like a completion callback that cancels the watchdog from the worker's
        // finally block before the worker future itself is marked done.
        Future<Boolean> worker = new CompletableFuture<>();
        Thread watchdog = new Thread(() -> service.watchTask(worker, 60L, "task-1", "proc-1"));
        watchdog.start();

        long deadline = System.currentTimeMillis() + 5000;
        while (watchdog.getState() != Thread.State.TIMED_WAITING && System.currentTimeMillis() < deadline) {
            Thread.sleep(10);
        }
        Assertions.assertEquals(Thread.State.TIMED_WAITING, watchdog.getState(),
                "the watchdog must be parked in future.get before the interrupt");

        watchdog.interrupt();
        watchdog.join(5000);
        Assertions.assertFalse(watchdog.isAlive(), "the watchdog must stop after the interrupt");

        Assertions.assertEquals(TaskState.COMPLETED,
                orchestrator.getTaskInstanceRepository().getTaskInstance("task-1").getState(),
                "an interrupted watchdog must not mark a completed task FAILED");
        Assertions.assertEquals(TaskState.COMPLETED,
                orchestrator.getProcessInstanceRepository().getProcess("proc-1").getState(),
                "an interrupted watchdog must not mark the process FAILED");
        Assertions.assertFalse(worker.isCancelled(), "the worker future must not be cancelled");
    }

    @Test
    void timedOutWatchdogMarksTaskAndProcessFailed() {
        seedTaskAndProcess(TaskState.IN_PROGRESS);

        Future<Boolean> worker = new CompletableFuture<>();
        service.watchTask(worker, 1L, "task-1", "proc-1");

        Assertions.assertEquals(TaskState.FAILED,
                orchestrator.getTaskInstanceRepository().getTaskInstance("task-1").getState());
        Assertions.assertEquals(TaskState.FAILED,
                orchestrator.getProcessInstanceRepository().getProcess("proc-1").getState());
        Assertions.assertTrue(worker.isCancelled(), "the hung worker must be cancelled on timeout");
    }
}

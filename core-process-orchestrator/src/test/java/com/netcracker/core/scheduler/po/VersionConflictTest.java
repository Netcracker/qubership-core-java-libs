package com.netcracker.core.scheduler.po;

import com.netcracker.core.scheduler.helpers.SchedulerUtils;
import com.netcracker.core.scheduler.po.model.pojo.ProcessInstanceImpl;
import com.netcracker.core.scheduler.po.model.pojo.TaskInstanceImpl;
import com.netcracker.core.scheduler.po.repository.ContextRepository;
import com.netcracker.core.scheduler.po.repository.ProcessInstanceRepository;
import com.netcracker.core.scheduler.po.repository.TaskInstanceRepository;
import com.netcracker.core.scheduler.po.repository.VersionMismatchException;
import com.netcracker.core.scheduler.po.repository.impl.ContextRepositoryImpl;
import com.netcracker.core.scheduler.po.repository.impl.ProcessInstanceRepositoryImpl;
import com.netcracker.core.scheduler.po.repository.impl.TaskInstanceRepositoryImpl;
import com.netcracker.core.scheduler.po.runnable.TaskExecutionWrapper;
import com.netcracker.core.scheduler.po.samples.tasks.DummyTask2;
import com.netcracker.core.scheduler.po.task.TaskState;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Covers the optimistic-lock behavior under interleaved writers: two in-memory
 * copies of the same row exist (the process tick, the executing task, the
 * failure handler, and the timeout watchdog all load their own), one saves
 * first, and the loser must either fail atomically or recover by reloading.
 */
class VersionConflictTest {

    DataSource dataSource;

    @BeforeEach
    void setup() {
        dataSource = SchedulerUtils.initDatabase();
    }

    @AfterEach
    void teardown() {
        ((HikariDataSource) dataSource).close();
    }

    private TaskInstanceImpl storedTask(TaskInstanceRepository repository) {
        TaskInstanceImpl task = new TaskInstanceImpl("task-1", "TestTask", DummyTask2.class.getName(), "proc-1");
        task.setState(TaskState.NOT_STARTED);
        repository.putTaskInstance(task);
        return task;
    }

    @Test
    void staleTaskCopyFailsWithoutCorruptingTheRow() {
        TaskInstanceRepository repository = new TaskInstanceRepositoryImpl(dataSource);
        storedTask(repository);

        TaskInstanceImpl copyA = repository.getTaskInstance("task-1");
        TaskInstanceImpl copyB = repository.getTaskInstance("task-1");

        copyA.setState(TaskState.IN_PROGRESS);
        repository.putTaskInstance(copyA);

        copyB.setState(TaskState.COMPLETED);
        Assertions.assertThrows(VersionMismatchException.class, () -> repository.putTaskInstance(copyB));

        TaskInstanceImpl stored = repository.getTaskInstance("task-1");
        Assertions.assertEquals(TaskState.IN_PROGRESS, stored.getState(), "the winning write must survive");
        Assertions.assertEquals(copyA.getVersion(), stored.getVersion());
    }

    @Test
    void staleProcessCopyFailsWithoutCorruptingTheRow() {
        ProcessInstanceRepository repository = new ProcessInstanceRepositoryImpl(dataSource);
        ProcessInstanceImpl process = new ProcessInstanceImpl("Test Instance", "proc-1", "def-1");
        process.setState(TaskState.NOT_STARTED);
        repository.putProcessInstance(process);

        ProcessInstanceImpl copyA = repository.getProcess("proc-1");
        ProcessInstanceImpl copyB = repository.getProcess("proc-1");

        copyA.setState(TaskState.IN_PROGRESS);
        repository.putProcessInstance(copyA);

        copyB.setState(TaskState.FAILED);
        Assertions.assertThrows(VersionMismatchException.class, () -> repository.putProcessInstance(copyB));

        Assertions.assertEquals(TaskState.IN_PROGRESS, repository.getProcess("proc-1").getState());
    }

    @Test
    void staleContextCopyFailsWithoutCorruptingTheRow() {
        ContextRepository repository = new ContextRepositoryImpl(dataSource);
        DataContext context = new DataContext("ctx-1");
        context.setRepository(repository);
        context.put("k", "v0");
        repository.putContext(context);

        DataContext copyA = repository.getContext("ctx-1");
        copyA.setRepository(repository);
        DataContext copyB = repository.getContext("ctx-1");
        copyB.setRepository(repository);

        copyA.put("k", "vA");
        repository.putContext(copyA);

        copyB.put("k", "vB");
        Assertions.assertThrows(VersionMismatchException.class, () -> repository.putContext(copyB));

        Assertions.assertEquals("vA", repository.getContext("ctx-1").get("k"));
    }

    @Test
    void saveResolvingConflictReappliesTheTerminalStateOnAFreshCopy() throws Exception {
        // save()/reload() resolve the repository through the orchestrator singleton,
        // so bootstrap a real one the same way SchedulerTest does.
        ProcessOrchestrator orchestrator = new ProcessOrchestrator(dataSource);
        try {
            TaskInstanceRepository repository = orchestrator.getTaskInstanceRepository();
            storedTask(repository);

            TaskInstanceImpl worker = repository.getTaskInstance("task-1");
            TaskInstanceImpl tick = repository.getTaskInstance("task-1");

            // The process tick wins the race with an unrelated bump.
            tick.setState(TaskState.IN_PROGRESS);
            repository.putTaskInstance(tick);

            // The worker's terminal save must recover and win.
            worker.setState(TaskState.COMPLETED);
            TaskInstanceImpl persisted = worker.saveResolvingConflict(t -> t.setState(TaskState.COMPLETED));

            Assertions.assertEquals(TaskState.COMPLETED, persisted.getState());
            Assertions.assertEquals(TaskState.COMPLETED, repository.getTaskInstance("task-1").getState());
        } finally {
            orchestrator.stop();
        }
    }

    @Test
    void contextApplyRecoversFromAConcurrentWriter() {
        ContextRepository repository = new ContextRepositoryImpl(dataSource);
        DataContext seed = new DataContext("ctx-1");
        seed.setRepository(repository);
        seed.put("seed", "1");
        repository.putContext(seed);

        DataContext worker = repository.getContext("ctx-1");
        worker.setRepository(repository);
        DataContext tick = repository.getContext("ctx-1");
        tick.setRepository(repository);

        tick.put("tick", "yes");
        repository.putContext(tick);

        worker.apply(c -> c.put("stateDescription", "Done"));

        DataContext stored = repository.getContext("ctx-1");
        Assertions.assertEquals("Done", stored.get("stateDescription"), "the re-applied mutation must land");
        Assertions.assertEquals("yes", stored.get("tick"), "the concurrent writer's data must survive");
    }

    @Test
    void wrapperRunsTheCallbackExactlyOnceOnFailure() throws Exception {
        AtomicInteger callbacks = new AtomicInteger();
        TaskExecutionWrapper wrapper = new TaskExecutionWrapper(
                () -> { throw new IllegalStateException("boom"); },
                callbacks::incrementAndGet);

        Assertions.assertEquals(Boolean.TRUE, wrapper.call());
        Assertions.assertEquals(1, callbacks.get());
    }

    @Test
    void wrapperRunsTheCallbackExactlyOnceOnSuccess() throws Exception {
        AtomicInteger callbacks = new AtomicInteger();
        TaskExecutionWrapper wrapper = new TaskExecutionWrapper(() -> { }, callbacks::incrementAndGet);

        Assertions.assertEquals(Boolean.TRUE, wrapper.call());
        Assertions.assertEquals(1, callbacks.get());
    }
}

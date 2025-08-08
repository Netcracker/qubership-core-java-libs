package com.netcracker.core.scheduler.po;


import com.github.kagkarlsson.scheduler.Scheduler;
import com.zaxxer.hikari.HikariDataSource;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.qubership.core.scheduler.SchedulerConfig;
import org.qubership.core.scheduler.helpers.MyTestObject;
import org.qubership.core.scheduler.helpers.SchedulerUtils;
import org.qubership.core.scheduler.po.model.pojo.ProcessInstanceImpl;
import org.qubership.core.scheduler.po.model.pojo.TaskInstanceImpl;
import org.qubership.core.scheduler.po.repository.TaskInstanceRepository;
import org.qubership.core.scheduler.po.samples.DummyAsyncProcess;
import org.qubership.core.scheduler.po.samples.DymmyProcess;
import org.qubership.core.scheduler.po.samples.FailedDymmyProcess;
import org.qubership.core.scheduler.po.samples.tasks.*;
import org.qubership.core.scheduler.po.task.NamedTask;
import org.qubership.core.scheduler.po.task.TaskState;

import javax.sql.DataSource;
import java.lang.reflect.InvocationTargetException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.function.Function;

import static org.awaitility.Awaitility.await;


class SchedulerTest {

    DataSource dataSource;
    ProcessOrchestrator orchestrator;

    @BeforeEach
    public void setup() throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        dataSource = SchedulerUtils.initDatabase();
        orchestrator = new ProcessOrchestrator(dataSource);
    }

    @AfterEach
    public void teardown() {
        ((HikariDataSource) dataSource).close();
        orchestrator.stop();
    }

    @Test
    void initTest() throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        Scheduler scheduler = SchedulerConfig.getScheduler(dataSource);
        scheduler.stop();
        Assertions.assertNotNull(scheduler);
    }

    @Test
    void testFailedDataTask() {

        ProcessDefinition warpUm = new FailedDymmyProcess();
        ProcessInstanceImpl process = orchestrator.createProcess(warpUm);
        process.getContext().apply((DataContext c) -> c.put("MyData1", "Test"));
        orchestrator.startProcess(process);
        waitForProcess(process.getId());
        process = orchestrator.getProcessInstance(process.getId());
        Assertions.assertTrue(process.getTasks().stream().allMatch(t -> t.getState().equals(TaskState.FAILED)));

        DataContext context = process.getContext();

        Assertions.assertTrue(context.containsKey("MyData1"), "MyData1");
        Assertions.assertTrue(context.containsKey("MyData2"), "MyData2");
        Assertions.assertTrue(context.containsKey("MyError"), "MyError");
        orchestrator.stop();
    }

    @Test
    void testFailedDataTask2() {

        ProcessDefinition warpUm = new FailedDymmyProcess();
        ProcessInstanceImpl process = orchestrator.createProcess(warpUm);
        process.getContext().apply((DataContext c) -> c.put("MyData1", "Test"));
        orchestrator.startProcess(process);
        waitForProcess(process.getId());
        process = orchestrator.getProcessInstance(process.getId());
        Assertions.assertTrue(process.getTasks().stream().allMatch(t -> t.getState().equals(TaskState.FAILED)));

        DataContext context = process.getContext();

        Assertions.assertTrue(context.containsKey("MyData1"), "MyData1");
        Assertions.assertTrue(context.containsKey("MyData2"), "MyData2");
        Assertions.assertTrue(context.containsKey("MyError"), "MyError");
        orchestrator.stop();
    }

    @Test
    void testPO() {

        ProcessDefinition warpUm = new DymmyProcess();
        ProcessInstanceImpl process = orchestrator.createProcess(warpUm);
        orchestrator.startProcess(process);
        waitForProcess(process.getId());
        process = orchestrator.getProcessInstance(process.getId());
        Assertions.assertTrue(process.getTasks().stream().allMatch(t -> t.getState().equals(TaskState.COMPLETED)));
        orchestrator.stop();
    }

    @Test
    void testPODates() {
        ProcessDefinition warpUm = new DymmyProcess();
        ProcessInstanceImpl process = orchestrator.createProcess(warpUm);
        orchestrator.startProcess(process);
        waitForProcess(process.getId());
        process = orchestrator.getProcessInstance(process.getId());
        Assertions.assertTrue(process.getTasks().stream().allMatch(t -> t.getState().equals(TaskState.COMPLETED)));
        process = orchestrator.getProcessInstance(process.getId());
        Assertions.assertNotNull(process);
        Assertions.assertTrue(process.getEndTime().after(process.getStartTime()));
        orchestrator.stop();
        Assertions.assertNotNull(process.getProcessDefinitionID());
    }


    @SneakyThrows
    @Test
    void terminateSyncTest() {
        ProcessDefinition testProcess = new ProcessDefinition("SyncTest");
        testProcess.addTask(LongRunningTask.class);
        ProcessInstanceImpl instance = orchestrator.createProcess(testProcess);
        orchestrator.startProcess(instance);
        orchestrator.terminateProcess(instance.getId());
        waitForProcess(instance.getId());
        Assertions.assertEquals(TaskState.TERMINATED, orchestrator.getProcessInstance(instance.getId()).getState());
        orchestrator.stop();
    }

    @SneakyThrows
    @Test
    void terminateSyncTestMulti() {
        ProcessDefinition testProcess = new ProcessDefinition("SyncTest");
        for (int i = 0; i < 20; i++) {
            testProcess.addTask(new NamedTask(LongRunningTask.class, String.format("Task %d", i)));
        }
        ProcessInstanceImpl instance = orchestrator.createProcess(testProcess);
        orchestrator.startProcess(instance);
        orchestrator.terminateProcess(instance.getId());
        waitForProcess(instance.getId());
        Assertions.assertEquals(TaskState.TERMINATED, orchestrator.getProcessInstance(instance.getId()).getState());
        orchestrator.stop();
    }

    @SneakyThrows
    @Test
    void terminateSyncWithRetryTest() {
        ProcessDefinition testProcess = new ProcessDefinition("SyncTest");
        testProcess.addTask(LongRunningTask.class);
        ProcessInstanceImpl instance = orchestrator.createProcess(testProcess);
        orchestrator.startProcess(instance);
        orchestrator.terminateProcess(instance.getId());
        waitForProcess(instance.getId());
        Assertions.assertEquals(TaskState.TERMINATED, orchestrator.getProcessInstance(instance.getId()).getState());
        instance = orchestrator.getProcessInstance(instance.getId());
        orchestrator.retryProcess(instance);
        waitForProcessCondition(instance.getId(), this::isInProgress);
        orchestrator.stop();
    }

    @Test
    void testDataPO() {

        ProcessDefinition warpUm = new DataTestPO();
        ProcessInstanceImpl process = orchestrator.createProcess(warpUm);
        process.getContext().apply((DataContext c) -> c.put("MyData", "Test"));

        orchestrator.startProcess(process);
        waitForProcess(process.getId());
        process = orchestrator.getProcessInstance(process.getId());
        Assertions.assertTrue(process.getTasks().stream().allMatch(t -> t.getState().equals(TaskState.COMPLETED)));
        orchestrator.stop();
    }

    @Test
    void testAsyncPO() {

        ProcessDefinition warpUm = new DummyAsyncProcess();
        ProcessInstanceImpl process = orchestrator.createProcess(warpUm);

        orchestrator.startProcess(process);
        waitForProcess(process.getId());
        process = orchestrator.getProcessInstance(process.getId());
        Assertions.assertTrue(process.getTasks().stream().allMatch(t -> t.getState().equals(TaskState.COMPLETED)));
        orchestrator.stop();
        Assertions.assertEquals("1", process.getContext().get("Test"), "Async task Synchronous part not started");
    }

    @Test
    void testNamedTaskPO() {
        ProcessDefinition warpUm = new DummyNamedProcess();
        ProcessInstanceImpl process = orchestrator.createProcess(warpUm);

        orchestrator.startProcess(process);
        waitForProcess(process.getId());
        process = orchestrator.getProcessInstance(process.getId());
        Assertions.assertTrue(process.getTasks().stream().allMatch(t -> t.getState().equals(TaskState.COMPLETED)));
        orchestrator.stop();
    }


    @SneakyThrows
    @Test
    void testRetry() {
        ProcessDefinition retryProcess = new ProcessDefinition("RetryProcess").addTask(TaskForRetry.class);
        ProcessInstanceImpl process = orchestrator.createProcess(retryProcess);
        orchestrator.startProcess(process);
        waitForProcess(process.getId());
        process = orchestrator.getProcessInstance(process.getId());
        Assertions.assertTrue(process.getTasks().stream().allMatch(t -> t.getState().equals(TaskState.FAILED)));
        process.getContext().apply((DataContext c) -> c.put("isFail", Boolean.FALSE));
        orchestrator.retryProcess(process);
        process = orchestrator.getProcessInstance(process.getId());
        Assertions.assertEquals(TaskState.IN_PROGRESS, process.getState());
        waitForProcess(process.getId());
        process = orchestrator.getProcessInstance(process.getId());
        Assertions.assertTrue(process.getTasks().stream().allMatch(t -> t.getState().equals(TaskState.COMPLETED)));
        orchestrator.stop();
    }

    @Test
    void testPOTaskDates() {

        ProcessDefinition warpUm = new DymmyProcess();
        ProcessInstanceImpl process = orchestrator.createProcess(warpUm);
        orchestrator.startProcess(process);
        process.getContext().apply(t -> {
            MyTestObject d = new MyTestObject();
            d.setData("xxx");
            t.put("Test", d);
        });
        waitForProcess(process.getId());
        process = orchestrator.getProcessInstance(process.getId());
        Assertions.assertTrue(process.getTasks().stream().allMatch(t -> t.getState().equals(TaskState.COMPLETED)));
        process = orchestrator.getProcessInstance(process.getId());
        Assertions.assertNotNull(process);
        Assertions.assertTrue(process.getEndTime().after(process.getStartTime()));
        process.getTasks().forEach(TaskInstanceImpl::getStartTime);
        MyTestObject d = (MyTestObject) process.getContext().get("Test");
        Assertions.assertEquals("xxx", d.getData());
        orchestrator.stop();
        Assertions.assertNotNull(process.getProcessDefinitionID());
    }

    @Test
    void testTimeoutSet() {
        TaskInstanceRepository repository = orchestrator.getTaskInstanceRepository();
        TaskInstanceImpl taskInstance = new TaskInstanceImpl("10", "TestTask", "CC", "12");
        taskInstance.setTimeout(100L);
        repository.putTaskInstance(taskInstance);
        taskInstance = repository.getTaskInstance("10");
        Assertions.assertEquals("TestTask", taskInstance.getName());
        Assertions.assertEquals(100L, taskInstance.getTimeout());
    }

    @Test
    void testAsyncTimeoutSet() {
        TaskInstanceRepository repository = orchestrator.getTaskInstanceRepository();
        TaskInstanceImpl taskInstance = new TaskInstanceImpl("10", "TestTask", "CC", "12");
        taskInstance.setAsyncTimeout(100L);
        repository.putTaskInstance(taskInstance);
        taskInstance = repository.getTaskInstance("10");
        Assertions.assertEquals("TestTask", taskInstance.getName());
        Assertions.assertEquals(100L, taskInstance.getAsyncTimeout());
    }

    @Test
    void terminateSyncTimeout() {
        ProcessDefinition testProcess = new ProcessDefinition("SyncTest");
        NamedTask task = new NamedTask(LongRunningTask.class.getName(), LongRunningTask.class.getName(), 1L);
        testProcess.addTask(task);
        ProcessInstanceImpl instance = orchestrator.createProcess(testProcess);
        orchestrator.startProcess(instance);

        waitForProcess(instance.getId());
        Assertions.assertEquals(TaskState.FAILED, orchestrator.getProcessInstance(instance.getId()).getState());
        orchestrator.stop();
    }

    @Test
    void terminateAsyncTimeout() {
        ProcessDefinition testProcess = new ProcessDefinition("AsyncTest");
        NamedTask task = new NamedTask(AsyncTimeoutTask.class.getName(), AsyncTimeoutTask.class.getName(), 5L, 10L);
        testProcess.addTask(task);
        ProcessInstanceImpl instance = orchestrator.createProcess(testProcess);
        orchestrator.startProcess(instance);

        waitForProcess(instance.getId());
        Assertions.assertEquals(TaskState.FAILED, orchestrator.getProcessInstance(instance.getId()).getState());
        orchestrator.stop();
    }

    private void waitForProcess(String id) {
        waitForProcessCondition(id, this::isStopped);
    }

    private void waitForProcessCondition(String id, Function<String, Boolean> condition) {
        await()
                .atMost(Duration.ofSeconds(60))
                .pollDelay(Duration.ZERO)
                .pollInterval(Duration.of(200, ChronoUnit.MILLIS))
                .until(() -> condition.apply(id));
    }

    private boolean isInProgress(String id) {
        ProcessInstanceImpl instance = orchestrator.getProcessInstance(id);
        return instance.getTasks().stream().anyMatch(t -> t.getState().equals(TaskState.IN_PROGRESS) || t.getState().equals(TaskState.NOT_STARTED));
    }

    private boolean isStopped(String id) {
        ProcessInstanceImpl instance = orchestrator.getProcessInstance(id);
        if (instance == null) {
            return true;
        }
        if (instance.getTasks().stream().anyMatch(t -> t.getState().equals(TaskState.FAILED))) {
            return instance.getState() == TaskState.FAILED;
        }
        if (instance.getTasks().stream().anyMatch(t -> t.getState().equals(TaskState.TERMINATED))) {
            return instance.getState() == TaskState.TERMINATED;
        }
        if (instance.getState() == TaskState.TERMINATED) {
            return true;
        }
        boolean isProgressingTask = instance.getTasks().stream().anyMatch(t -> t.getState().equals(TaskState.IN_PROGRESS) || t.getState().equals(TaskState.NOT_STARTED));
        return !isProgressingTask && instance.getState() == TaskState.COMPLETED;
    }

}

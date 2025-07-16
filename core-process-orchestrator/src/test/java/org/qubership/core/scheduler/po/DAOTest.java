package org.qubership.core.scheduler.po;

import org.qubership.core.scheduler.helpers.SchedulerUtils;
import org.qubership.core.scheduler.po.model.pojo.ProcessInstanceImpl;
import org.qubership.core.scheduler.po.model.pojo.TaskInstanceImpl;
import org.qubership.core.scheduler.po.repository.ProcessInstanceRepository;
import org.qubership.core.scheduler.po.repository.TaskInstanceRepository;
import org.qubership.core.scheduler.po.repository.impl.ProcessInstanceRepositoryImpl;
import org.qubership.core.scheduler.po.repository.impl.TaskInstanceRepositoryImpl;
import org.qubership.core.scheduler.po.samples.tasks.DummyTask2;
import org.qubership.core.scheduler.po.task.TaskState;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.util.Calendar;
import java.util.List;

class DAOTest {

    DataSource dataSource;

    @BeforeEach
    public void setup() {
        dataSource = SchedulerUtils.initDatabase();
    }

    @AfterEach
    public void teardown() {
        ((HikariDataSource) dataSource).close();
    }

    @Test
    void testProcessInstanceRepository() {
        ProcessInstanceRepository repository = new ProcessInstanceRepositoryImpl(dataSource);
        Assertions.assertNull(repository.getProcess("111"));
    }

    @Test
    void testPutProcessInstance() {
        ProcessInstanceRepository repository = new ProcessInstanceRepositoryImpl(dataSource);
        ProcessInstanceImpl processInstance = new ProcessInstanceImpl("Test Instance", "1", "2");
        processInstance.setState(TaskState.IN_PROGRESS);
        processInstance.setStartTime(Calendar.getInstance().getTime());
        processInstance.setEndTime(Calendar.getInstance().getTime());


        repository.putProcessInstance(processInstance);

        ProcessInstanceImpl newProcess = repository.getProcess("1");
        Assertions.assertEquals(newProcess, processInstance);
    }


    @Test
    void testPutAndUpdateProcessInstance() {
        ProcessInstanceRepository repository = new ProcessInstanceRepositoryImpl(dataSource);
        ProcessInstanceImpl processInstance = new ProcessInstanceImpl("Test Instance", "1", "2");
        processInstance.setState(TaskState.IN_PROGRESS);
        processInstance.setStartTime(Calendar.getInstance().getTime());
        processInstance.setEndTime(Calendar.getInstance().getTime());


        repository.putProcessInstance(processInstance);

        ProcessInstanceImpl newProcess = repository.getProcess("1");
        Assertions.assertEquals(newProcess, processInstance);
        processInstance.setEndTime(Calendar.getInstance().getTime());
        repository.putProcessInstance(processInstance);
        newProcess = repository.getProcess("1");
        Assertions.assertEquals(newProcess, processInstance);
    }

    @Test
    void testTaskRepository() {
        TaskInstanceRepository taskInstanceRepository = new TaskInstanceRepositoryImpl(dataSource);
        Assertions.assertNull(taskInstanceRepository.getTaskInstance("1111"));
    }

    @Test
    void testPutTaskRepository() {
        TaskInstanceRepository taskInstanceRepository = new TaskInstanceRepositoryImpl(dataSource);
        TaskInstanceImpl taskInstance = new TaskInstanceImpl("1", "TestTask", DummyTask2.class.getName(), "2");

        taskInstance.setState(TaskState.IN_PROGRESS);

        taskInstanceRepository.putTaskInstance(taskInstance);
        TaskInstanceImpl newTask = taskInstanceRepository.getTaskInstance("1");


        Assertions.assertEquals(taskInstance, newTask);

    }

    @Test
    void testPutAndUpdateTaskRepository() {
        TaskInstanceRepository taskInstanceRepository = new TaskInstanceRepositoryImpl(dataSource);
        TaskInstanceImpl taskInstance = new TaskInstanceImpl("1", "TestTask", DummyTask2.class.getName(), "2");

        taskInstance.setState(TaskState.IN_PROGRESS);

        taskInstanceRepository.putTaskInstance(taskInstance);
        TaskInstanceImpl newTask = taskInstanceRepository.getTaskInstance("1");
        Assertions.assertEquals(taskInstance, newTask);

        taskInstance.setState(TaskState.TERMINATED);
        taskInstanceRepository.putTaskInstance(taskInstance);
        newTask = taskInstanceRepository.getTaskInstance("1");
        Assertions.assertEquals(taskInstance, newTask);

    }

    @Test
    void testAddTasksInBulkRepository() {
        TaskInstanceRepository taskInstanceRepository = new TaskInstanceRepositoryImpl(dataSource);

        TaskInstanceImpl taskInstance1 = new TaskInstanceImpl("1", "TestTask1", DummyTask2.class.getName(), "2");
        TaskInstanceImpl taskInstance2 = new TaskInstanceImpl("2", "TestTask2", DummyTask2.class.getName(), "2");
        taskInstance1.setState(TaskState.NOT_STARTED);
        taskInstance2.setState(TaskState.IN_PROGRESS);

        taskInstanceRepository.addTaskInstancesBulk(List.of(taskInstance1, taskInstance2));
        TaskInstanceImpl newTask1 = taskInstanceRepository.getTaskInstance("1");
        TaskInstanceImpl newTask2 = taskInstanceRepository.getTaskInstance("2");

        Assertions.assertEquals(taskInstance1, newTask1);
        Assertions.assertEquals(taskInstance2, newTask2);
    }
}

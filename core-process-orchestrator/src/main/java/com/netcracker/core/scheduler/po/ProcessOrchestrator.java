package com.netcracker.core.scheduler.po;

import com.github.kagkarlsson.scheduler.ScheduledExecution;
import com.github.kagkarlsson.scheduler.Scheduler;
import com.github.kagkarlsson.scheduler.task.Task;
import com.netcracker.core.scheduler.helpers.TaskExecutorService;
import com.netcracker.core.scheduler.po.context.ProcessContext;
import com.netcracker.core.scheduler.po.model.pojo.ProcessInstanceImpl;
import com.netcracker.core.scheduler.po.model.pojo.TaskInstanceImpl;
import com.netcracker.core.scheduler.po.repository.ContextRepository;
import com.netcracker.core.scheduler.po.repository.ProcessInstanceRepository;
import com.netcracker.core.scheduler.po.repository.TaskInstanceRepository;
import com.netcracker.core.scheduler.po.repository.impl.ContextRepositoryImpl;
import com.netcracker.core.scheduler.po.repository.impl.ProcessInstanceRepositoryImpl;
import com.netcracker.core.scheduler.po.repository.impl.TaskInstanceRepositoryImpl;
import com.netcracker.core.scheduler.po.serializers.ExtendedSerializer;
import com.netcracker.core.scheduler.po.serializers.JsonPOSerializer;
import com.netcracker.core.scheduler.po.task.TaskState;
import com.netcracker.core.scheduler.po.task.templates.AbstractProcessTask;
import lombok.SneakyThrows;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.sql.DataSource;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class ProcessOrchestrator {

    private static ProcessOrchestrator instance;
    private TaskExecutorService executorService;

    private Scheduler scheduler;
    private ContextRepository contextRepository;
    private ProcessInstanceRepository processInstanceRepository;
    private TaskInstanceRepository taskInstanceRepository;

    private static final Logger logger = LoggerFactory.getLogger(ProcessOrchestrator.class);

    public ProcessOrchestrator(DataSource dataSource) throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        this(dataSource, 10);

    }

    public ProcessOrchestrator(DataSource dataSource, Integer threads) throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        this(dataSource, threads, getTasks());
    }

    @SneakyThrows
    public ProcessOrchestrator(DataSource dataSource, Integer threads, List<Task<?>> tasks) {
        ExtendedSerializer custom = new JsonPOSerializer();
        List<Task<?>> knownTasks = new ArrayList<>(tasks);
        executorService = new TaskExecutorService(threads);
        knownTasks.add(new Process());
        scheduler = Scheduler.create(dataSource, knownTasks)
                .enableImmediateExecution()
                .pollingInterval(Duration.ofSeconds(2))
                .registerShutdownHook()
                .shutdownMaxWait(Duration.ofSeconds(10))
                .heartbeatInterval(Duration.ofSeconds(20))
                .serializer(custom)
                .threads(threads)
                .executorService(executorService)
                .build();
        scheduler.start();
        instance = this;
        contextRepository = new ContextRepositoryImpl(dataSource, custom);
        processInstanceRepository = new ProcessInstanceRepositoryImpl(dataSource);
        taskInstanceRepository = new TaskInstanceRepositoryImpl(dataSource, custom);
    }

    public ProcessInstanceImpl createProcess(ProcessDefinition processDefinition) {
        ProcessInstanceImpl processInstance = processDefinition.createInstance();
        processInstanceRepository.putProcessInstance(processInstance);
        return processInstance;
    }

    public void startProcess(ProcessInstanceImpl processInstance) {
        processInstance.setStartTime(Calendar.getInstance().getTime());
        processInstance.setState(TaskState.IN_PROGRESS);
        processInstance.save();
        ProcessContext context = new ProcessContext(processInstance.getId());
        scheduler.schedule(new com.github.kagkarlsson.scheduler.task.TaskInstance<>("POProcess", processInstance.getId(), context), Instant.now());
    }

    public static ScheduledExecution<Object> getTask(String type, String id) {
        return instance.scheduler.getScheduledExecution(new com.github.kagkarlsson.scheduler.task.TaskInstance<>(type, id)).orElse(null);
    }

    @Nullable
    public ProcessContext getProcess(String id) {
        ScheduledExecution<Object> task = getTask(Process.PO_TASK_NAME, id);
        if (task == null) return null;
        return (ProcessContext) task.getData();
    }

    // Need for test Only, to not share Engine instance between test
    public void stop() {
        scheduler.stop();
    }

    public static List<Task<?>> getTasks() throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        List<Task<?>> knownTasks = new ArrayList<>();
        Reflections reflections = new Reflections("com.netcracker");

        Set<Class<? extends AbstractProcessTask>> subTypes = reflections.getSubTypesOf(AbstractProcessTask.class);


        for (Class<? extends AbstractProcessTask> c : subTypes)
            if (!Modifier.isAbstract(c.getModifiers()))
                knownTasks.add(c.getDeclaredConstructor().newInstance());
        return knownTasks;
    }


    public DataContext getDataContext(String id) {
        DataContext context = contextRepository.getContext(id);
        if (context == null) {
            context = new DataContext(id);
            contextRepository.putContext(context);
        }
        context.setRepository(contextRepository);
        return context;
    }

    public static ProcessOrchestrator getInstance() {
        return instance;
    }

    public void retryProcess(ProcessInstanceImpl processInstance) {
        processInstance.setState(TaskState.IN_PROGRESS);
        for (TaskInstanceImpl taskInstance : processInstance.getTasks()) {
            if (taskInstance.getState() == TaskState.FAILED || taskInstance.getState() == TaskState.TERMINATED) {
                taskInstance.setState(TaskState.IN_PROGRESS);
                DataContext context = taskInstance.getContext();
                List<String> keyToRemove = context
                        .keySet()
                        .stream()
                        .filter(t -> t.endsWith("-syncPartFlag"))
                        .toList();
                keyToRemove.forEach(k -> context.remove(k));
                context.save();
                taskInstance.setState(TaskState.NOT_STARTED);
                taskInstance.setStartTime(Calendar.getInstance().getTime());
                taskInstance.save();
                logger.info(
                        "Retry Task {} with id {} for process {} with id {}"
                        , taskInstance.getName()
                        , taskInstance.getId()
                        , processInstance.getName()
                        , processInstance.getId()
                );
            }

        }
        processInstance.save();
        ProcessContext context = new ProcessContext(processInstance.getId());
        scheduler.schedule(new com.github.kagkarlsson.scheduler.task.TaskInstance<>("POProcess", processInstance.getId(), context), Instant.now());
    }


    private Future<?> terminateTask(String type, String id) {
        logger.info("Send interruption to task: {}", id);
        Future<?> f = executorService.terminate(id);
        int i = 0;
        while (i < 5) {
            i++;
            ScheduledExecution task = getTask(type, id);
            if (task != null) {
                try {
                    scheduler.cancel(task.getTaskInstance());
                    i = 6;
                } catch (Exception e) {
                    if (i > 5)
                        logger.error("Error during task removal {}", id, e);
                } finally {

                }
            }
        }
        return f;
    }

    public void terminateProcess(String processId) {
        try {
            ProcessInstanceImpl processInstance = getProcessInstance(processId);
            terminateTask(Process.PO_TASK_NAME, processId).get();
            processInstance.getTasks().stream().filter(t -> t.getState() == TaskState.IN_PROGRESS || t.getState() == TaskState.NOT_STARTED).map(t -> {
                        Future<?> f = terminateTask(t.getType(), t.getId());
                        t = t.reload();
                        t.setState(TaskState.TERMINATED);
                        t.save();
                        return f;
                    }
            ).forEach(t -> {
                try {
                    t.get();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                } catch (ExecutionException e) {
                    throw new RuntimeException(e);
                }
            });
            processInstance = processInstance.reload();
            processInstance.setState(TaskState.TERMINATED);
            processInstance.save();
        } catch (Exception e) {
            logger.error("Error while terminating task", e);
        } finally {
        }
    }

    public ProcessInstanceImpl getProcessInstance(String id) {
        return processInstanceRepository.getProcess(id);
    }

    public ProcessInstanceRepository getProcessInstanceRepository() {
        return processInstanceRepository;
    }

    public TaskInstanceRepository getTaskInstanceRepository() {
        return taskInstanceRepository;
    }
}

package com.netcracker.core.scheduler.po;

import com.netcracker.core.scheduler.po.model.pojo.ProcessInstanceImpl;
import com.netcracker.core.scheduler.po.model.pojo.TaskInstanceImpl;
import com.netcracker.core.scheduler.po.repository.ContextRepository;
import com.netcracker.core.scheduler.po.repository.TaskInstanceRepository;
import com.netcracker.core.scheduler.po.task.NamedTask;
import com.netcracker.core.scheduler.po.task.templates.AbstractProcessTask;

import java.util.*;

public class ProcessDefinition {

    final String id;
    final String name;
    private final Map<NamedTask, List<NamedTask>> graph;
    private final Map<NamedTask, Map<String, Object>> taskContexts;

    public ProcessDefinition(String name) {
        this.name = name;
        this.id = UUID.randomUUID().toString();
        this.graph = new HashMap<>();
        this.taskContexts = new HashMap<>();
    }

    @SafeVarargs
    public final <T extends AbstractProcessTask> ProcessDefinition addTask(Class<T> task, Class<T>... dependsOn) {
        return addTask(new NamedTask(task, task.getName()), Arrays.stream(dependsOn).map(Class::getName).toArray(String[]::new));
    }

    public ProcessDefinition addTask(NamedTask taskName, String... dependsOn) {
        List<String> dependsList = new ArrayList<>(Arrays.stream(dependsOn).toList());
        graph.put(taskName, new ArrayList<>(graph.keySet().stream().filter(t -> dependsList.contains(t.getTaskName())).toList()));
        return this;
    }

    public ProcessDefinition addDepend(Class<? extends AbstractProcessTask> task, Class<? extends AbstractProcessTask> dependOn) {
        return addDepend(new NamedTask(task.getName(), task.getName()), dependOn.getName());
    }

    public ProcessDefinition addDepend(NamedTask taskName, String dependOn) {
        List<NamedTask> dependsOn = graph.computeIfAbsent(taskName, k -> new ArrayList<>());
        if (dependsOn.stream().noneMatch(t -> t.getTaskName().equals(dependOn)))
            dependsOn.addAll(graph.keySet().stream().filter(t -> t.getTaskName().equals(dependOn)).toList());
        return this;
    }

    public ProcessDefinition addTaskContext(NamedTask task, Map<String, Object> context) {
        taskContexts.put(task, context);
        return this;
    }

    public ProcessInstanceImpl createInstance() {
        ProcessInstanceImpl instance = new ProcessInstanceImpl(name, UUID.randomUUID().toString(), id);
        TaskInstanceRepository taskInstanceRepository = ProcessOrchestrator.getInstance().getTaskInstanceRepository();
        ContextRepository contextRepository = ProcessOrchestrator.getInstance().getContextRepository();

        List<DataContext> contexts = new ArrayList<>();
        List<TaskInstanceImpl> tasks = graph.entrySet().stream()
                .map(entry -> {
                    NamedTask namedTask = entry.getKey();
                    Long syncTimeOut = namedTask.getSyncTimeOut();
                    Long asyncTimeout = namedTask.getAsyncTimeout();
                    TaskInstanceImpl taskInstance = new TaskInstanceImpl(UUID.randomUUID().toString(), namedTask.getTaskName(), namedTask.getTaskClass(), instance.getId());
                    taskInstance.setDependsOn(entry.getValue());

                    DataContext taskContext = ProcessOrchestrator.getInstance().createDataContext(taskInstance.getId(),
                            dataContext -> TaskInstanceImpl.fillNewContext(dataContext, syncTimeOut, asyncTimeout));
                    Map<String, Object> customContext = ProcessDefinition.this.taskContexts.get(namedTask);
                    if (customContext != null) {
                        taskContext.putAll(customContext);
                    }
                    contexts.add(taskContext);
                    return taskInstance;
                }).toList();

        contextRepository.addContextsBulk(contexts);
        taskInstanceRepository.addTaskInstancesBulk(tasks);

        return instance;
    }
}

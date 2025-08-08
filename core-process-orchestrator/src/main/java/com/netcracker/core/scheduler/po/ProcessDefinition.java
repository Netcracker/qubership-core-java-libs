package com.netcracker.core.scheduler.po;

import com.netcracker.core.scheduler.po.model.pojo.ProcessInstanceImpl;
import com.netcracker.core.scheduler.po.model.pojo.TaskInstanceImpl;
import com.netcracker.core.scheduler.po.repository.TaskInstanceRepository;
import com.netcracker.core.scheduler.po.task.NamedTask;
import com.netcracker.core.scheduler.po.task.templates.AbstractProcessTask;

import java.util.*;

public class ProcessDefinition {

    String id;
    String name;
    private final Map<NamedTask, List<NamedTask>> graph;

    public ProcessDefinition(String name) {
        this.name = name;
        this.id = UUID.randomUUID().toString();
        graph = new HashMap<>();
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

    public ProcessInstanceImpl createInstance() {
        ProcessInstanceImpl instance = new ProcessInstanceImpl(name, UUID.randomUUID().toString(), id);
        TaskInstanceRepository taskInstanceRepository = ProcessOrchestrator.getInstance().getTaskInstanceRepository();
        taskInstanceRepository.addTaskInstancesBulk(graph.entrySet().stream()
                .map(task -> {
                    TaskInstanceImpl taskInstance = new TaskInstanceImpl(UUID.randomUUID().toString(), task.getKey().getTaskName(), task.getKey().getTaskClass(), instance.getId());
                    taskInstance.setTimeout(task.getKey().getSyncTimeOut());
                    taskInstance.setAsyncTimeout(task.getKey().getAsyncTimeout());
                    taskInstance.setDependsOn(task.getValue());
                    return taskInstance;
                }).toList());
        return instance;
    }


}

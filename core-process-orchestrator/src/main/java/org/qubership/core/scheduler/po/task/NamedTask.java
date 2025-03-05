package org.qubership.core.scheduler.po.task;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.qubership.core.scheduler.po.task.templates.AbstractProcessTask;
import lombok.Getter;

import java.io.Serializable;
import java.util.Objects;

@Getter
public class NamedTask implements Serializable {

    @JsonProperty("taskClass")
    private final String taskClass;
    @JsonProperty("taskName")
    private final String taskName;
    @JsonProperty("syncTimeOut")
    private final Long syncTimeOut;
    @JsonProperty("asyncTimeOut")
    private final Long asyncTimeout;


    public NamedTask(Class<?> taskClass) {
        this(taskClass.getName(), taskClass.getName(), 0L, 0L);
    }

    public NamedTask(String taskClass, String taskName, Long syncTimeOut) {
        this(taskClass, taskName, syncTimeOut, 0L);
    }

    public NamedTask(String taskClass, String taskName, Long syncTimeOut, Long asyncTimeout) {
        this.taskClass = taskClass;
        this.taskName = taskName;
        this.syncTimeOut = syncTimeOut;
        this.asyncTimeout = asyncTimeout;
    }

    @JsonCreator
    public NamedTask(@JsonProperty("taskClass") String taskClass, @JsonProperty("taskName") String taskName) {
        this(taskClass, taskName, 0L, 0L);
    }

    public <T extends AbstractProcessTask> NamedTask(Class<T> taskClass, String taskName) {
        this(taskClass.getName(), taskName, 0L, 0L);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o instanceof String) return o.equals(getTaskName());
        if (o == null || getClass() != o.getClass()) return false;
        NamedTask namedTask = (NamedTask) o;
        return Objects.equals(getTaskClass(), namedTask.getTaskClass()) && Objects.equals(getTaskName(), namedTask.getTaskName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getTaskName());
    }
}

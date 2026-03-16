package com.netcracker.core.scheduler.po.model.pojo;

import com.fasterxml.jackson.annotation.JsonIgnoreType;
import com.netcracker.core.scheduler.po.DataContext;
import com.netcracker.core.scheduler.po.ProcessOrchestrator;
import com.netcracker.core.scheduler.po.repository.TaskInstanceRepository;
import com.netcracker.core.scheduler.po.task.TaskState;
import lombok.Getter;

import java.util.Date;
import java.util.List;
import java.util.Objects;

@JsonIgnoreType
public class ProcessInstanceImpl {
    private boolean isDirty = false;

    @Getter
    private final String name;

    @Getter
    private final String id;

    @Getter
    private final String processDefinitionID;

    @Getter
    private Date startTime;

    @Getter
    private Date endTime;

    @Getter
    private TaskState state;


    @Getter
    private Integer version;


    public ProcessInstanceImpl(String name, String id, String processDefinitionID) {
        this(name, id, processDefinitionID, 0);
    }

    public ProcessInstanceImpl(String name, String id, String processDefinitionID, Integer version) {
        this.name = name;
        this.id = id;
        this.processDefinitionID = processDefinitionID;
        this.version = version;
        this.state = TaskState.NOT_STARTED;
    }
    /**/

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
        isDirty = true;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
        isDirty = true;
    }

    public void setState(TaskState state) {
        this.state = state;
        isDirty = true;
    }

    public List<TaskInstanceImpl> getTasks() {
        TaskInstanceRepository taskInstanceRepository = ProcessOrchestrator.getInstance().getTaskInstanceRepository();
        return taskInstanceRepository.getProcessTaskInstances(id);
    }


    /**/

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProcessInstanceImpl that = (ProcessInstanceImpl) o;
        return Objects.equals(getName(), that.getName()) && Objects.equals(getId(), that.getId()) && Objects.equals(getProcessDefinitionID(), that.getProcessDefinitionID()) && Objects.equals(getStartTime(), that.getStartTime()) && Objects.equals(getEndTime(), that.getEndTime()) && getState() == that.getState() && Objects.equals(getVersion(), that.getVersion());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName(), getId(), getProcessDefinitionID(), getStartTime(), getEndTime(), getState(), getVersion());
    }

    public void save() {
        if (isDirty)
            ProcessOrchestrator.getInstance().getProcessInstanceRepository().putProcessInstance(this);
    }

    public ProcessInstanceImpl reload()
    {
        return ProcessOrchestrator.getInstance().getProcessInstanceRepository().getProcess(id);
    }
    public void setVersion(Integer version) {
        this.version = version;
        isDirty = false;
    }

    public DataContext getContext() {
        return ProcessOrchestrator.getInstance().getDataContext(id);
    }
}

package org.qubership.core.scheduler.po.model.pojo;

import org.qubership.core.scheduler.po.DataContext;
import org.qubership.core.scheduler.po.ProcessOrchestrator;
import org.qubership.core.scheduler.po.task.NamedTask;
import org.qubership.core.scheduler.po.task.TaskState;
import lombok.Getter;

import java.util.Date;
import java.util.List;
import java.util.Objects;


public class TaskInstanceImpl {

    private boolean dirty;
    private DataContext context = null;
    private final String startTimeField;
    private final String endTimeField;

    @Getter
    private final String id;
    @Getter
    private String name;
    @Getter
    private String type;

    @Getter
    private List<NamedTask> dependsOn;

    @Getter
    private final String processID;
    @Getter
    private TaskState state;

    @Getter
    private int version;

    public TaskInstanceImpl(String id, String name, String type, String processID) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.state = TaskState.NOT_STARTED;
        this.processID = processID;
        dirty = false;
        startTimeField = String.format("startDate-%s", id);
        endTimeField = String.format("endDate-%s", id);
    }


    public void setName(String name) {
        this.name = name;
        dirty = true;
    }

    @SuppressWarnings("unused")
    public void setType(String type) {
        this.type = type;
        dirty = true;
    }

    public void setState(TaskState state) {
        if (this.state == state) return;

        this.state = state;
        dirty = true;
    }

    public void setVersion(int version) {
        this.version = version;
        dirty = false;
    }

    public void setDependsOn(List<NamedTask> dependsOn) {
        this.dependsOn = dependsOn;
        dirty = true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TaskInstanceImpl that = (TaskInstanceImpl) o;
        return getVersion() == that.getVersion() && Objects.equals(getId(), that.getId()) && Objects.equals(getName(), that.getName()) && Objects.equals(getType(), that.getType()) && Objects.equals(getProcessID(), that.getProcessID()) && getState() == that.getState();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), getName(), getType(), getProcessID(), getState(), getVersion());
    }

    public void save() {
        if (dirty)
            ProcessOrchestrator.getInstance().getTaskInstanceRepository().putTaskInstance(this);
    }

    public TaskInstanceImpl reload() {
        return ProcessOrchestrator.getInstance().getTaskInstanceRepository().getTaskInstance(id);
    }

    /**/

    public DataContext getContext() {
        if (context == null)
            context = ProcessOrchestrator.getInstance().getDataContext(id);
        return context;
    }

    @SuppressWarnings("unused")
    public Date getStartTime() {
        return (Date) getContext().get(startTimeField);
    }

    public void setStartTime(Date startTime) {
        getContext().apply((DataContext c) -> c.put(startTimeField, startTime));
    }

    @SuppressWarnings("unused")
    public Date getEndTime() {
        return (Date) getContext().get(endTimeField);
    }

    public void setEndTime(Date endTime) {
        getContext().apply((DataContext c) -> c.put(endTimeField, endTime));
    }

    public boolean isReady() {
        TaskState taskState = getState();
        ProcessInstanceImpl processInstance = ProcessOrchestrator.getInstance().getProcessInstance(getProcessID());
        return (taskState != TaskState.COMPLETED && taskState != TaskState.IN_PROGRESS && taskState != TaskState.FAILED) &&
                processInstance
                        .getTasks()
                        .stream()
                        .filter(t -> getDependsOn().contains(new NamedTask(t.getType(), t.getName())))
                        .map(TaskInstanceImpl::getState)
                        .allMatch(t -> t.equals(TaskState.COMPLETED));
    }

    public void setTimeout(Long timeout) {
        getContext().apply((DataContext c) -> c.put("_TAKS_TIME_OUT_", timeout));
    }

    public Long getTimeout() {
        Object val = getContext().get("_TAKS_TIME_OUT_");
        return val == null ? 0 : (Long) val;
    }

    public void setAsyncTimeout(Long timeout) {
        getContext().apply((DataContext c) -> c.put("_TAKS_ASYNC_TIME_OUT_", timeout));
    }

    public Long getAsyncTimeout() {
        Object val = getContext().get("_TAKS_ASYNC_TIME_OUT_");
        return val == null ? 0 : (Long) val;
    }
    /**/
}

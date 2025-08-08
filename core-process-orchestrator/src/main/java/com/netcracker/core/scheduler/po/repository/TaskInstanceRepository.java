package com.netcracker.core.scheduler.po.repository;

import com.netcracker.core.scheduler.po.model.pojo.TaskInstanceImpl;

import java.util.List;

public interface TaskInstanceRepository {

    void putTaskInstance(TaskInstanceImpl taskInstance);

    void addTaskInstancesBulk(List<TaskInstanceImpl> taskInstances);

    TaskInstanceImpl getTaskInstance(String id);

    List<TaskInstanceImpl> getProcessTaskInstances(String processId);

}

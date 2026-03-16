package com.netcracker.core.scheduler.po.repository;

import com.netcracker.core.scheduler.po.model.pojo.ProcessInstanceImpl;

public interface ProcessInstanceRepository {

    ProcessInstanceImpl getProcess(String id);

    void putProcessInstance(ProcessInstanceImpl processInstance);
}

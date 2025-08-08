package com.netcracker.core.scheduler.po.context;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.netcracker.core.scheduler.po.DataContext;
import com.netcracker.core.scheduler.po.ProcessOrchestrator;
import com.netcracker.core.scheduler.po.model.pojo.ProcessInstanceImpl;

@JsonIgnoreProperties(value = {"processInstance"})
public class ProcessContext {

    @JsonIgnore
    private ProcessInstanceImpl processInstance;

    @JsonProperty("id")
    private final String poId;

    @JsonCreator
    public ProcessContext(@JsonProperty("id") String id) {
        this.poId = id;
    }

    @JsonIgnore
    public DataContext getContext() {
        return ProcessOrchestrator.getInstance().getDataContext(processInstance.getId());
    }

    @JsonIgnore
    public ProcessInstanceImpl getProcess() {
        if (processInstance == null) processInstance = ProcessOrchestrator.getInstance().getProcessInstance(poId);
        return processInstance;
    }

}

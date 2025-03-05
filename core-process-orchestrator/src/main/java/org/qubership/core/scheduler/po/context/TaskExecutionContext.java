package org.qubership.core.scheduler.po.context;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.qubership.core.scheduler.po.ProcessOrchestrator;
import org.qubership.core.scheduler.po.model.pojo.ProcessInstanceImpl;
import org.qubership.core.scheduler.po.model.pojo.TaskInstanceImpl;


public class TaskExecutionContext {
    @JsonIgnore
    private ProcessInstanceImpl processInstance;
    @JsonIgnore
    private TaskInstanceImpl taskInstance;

    @JsonProperty("processId")
    private final String processId;
    @JsonProperty("taskId")
    private final String taskId;

    @JsonCreator
    public TaskExecutionContext(@JsonProperty("processId") String processId, @JsonProperty("taskId") String taskId) {
        this.processId = processId;
        this.taskId = taskId;
    }

    @JsonIgnore
    public ProcessInstanceImpl getProcess() {
        if (processInstance == null) processInstance = ProcessOrchestrator.getInstance().getProcessInstance(processId);
        return processInstance;
    }

    @JsonIgnore
    public TaskInstanceImpl getTaskInstance() {
        if (taskInstance == null)
            taskInstance = ProcessOrchestrator.getInstance().getTaskInstanceRepository().getTaskInstance(taskId);
        return taskInstance;
    }
}

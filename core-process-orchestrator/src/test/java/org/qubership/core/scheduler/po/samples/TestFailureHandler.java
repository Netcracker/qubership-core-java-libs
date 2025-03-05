package org.qubership.core.scheduler.po.samples;

import com.github.kagkarlsson.scheduler.task.ExecutionComplete;
import com.github.kagkarlsson.scheduler.task.ExecutionOperations;
import com.github.kagkarlsson.scheduler.task.FailureHandler;
import org.qubership.core.scheduler.po.DataContext;
import org.qubership.core.scheduler.po.context.TaskExecutionContext;
import org.qubership.core.scheduler.po.model.pojo.ProcessInstanceImpl;

public class TestFailureHandler implements FailureHandler<TaskExecutionContext> {
    @Override
    public void onFailure(ExecutionComplete executionComplete, ExecutionOperations<TaskExecutionContext> executionOperations) {
        ProcessInstanceImpl po = ((TaskExecutionContext) executionComplete.getExecution().taskInstance.getData()).getProcess();
        po.getContext().apply((DataContext c) -> c.put("MyError", "MyError"));
    }
}

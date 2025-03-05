package org.qubership.core.scheduler.po.samples;

import org.qubership.core.scheduler.po.ProcessDefinition;
import org.qubership.core.scheduler.po.samples.tasks.FailedDummyTask;

public class FailedDymmyProcess extends ProcessDefinition {
    public FailedDymmyProcess() {
        super("WarmUp");
        addTask(FailedDummyTask.class);
    }

}

package com.netcracker.core.scheduler.po.samples;

import com.netcracker.core.scheduler.po.ProcessDefinition;
import com.netcracker.core.scheduler.po.samples.tasks.FailedDummyTask;

public class FailedDymmyProcess extends ProcessDefinition {
    public FailedDymmyProcess() {
        super("WarmUp");
        addTask(FailedDummyTask.class);
    }

}

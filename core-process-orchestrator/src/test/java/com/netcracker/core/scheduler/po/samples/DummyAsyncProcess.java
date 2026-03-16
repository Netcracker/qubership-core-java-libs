package com.netcracker.core.scheduler.po.samples;

import com.netcracker.core.scheduler.po.ProcessDefinition;
import com.netcracker.core.scheduler.po.samples.tasks.DummyAsyncTask;

public class DummyAsyncProcess extends ProcessDefinition {
    public DummyAsyncProcess() {
        super("ASyncTest");
        addTask(DummyAsyncTask.class);
    }
}

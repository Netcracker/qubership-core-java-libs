package org.qubership.core.scheduler.po.samples;

import org.qubership.core.scheduler.po.ProcessDefinition;
import org.qubership.core.scheduler.po.samples.tasks.DummyAsyncTask;

public class DummyAsyncProcess extends ProcessDefinition {
    public DummyAsyncProcess() {
        super("ASyncTest");
        addTask(DummyAsyncTask.class);
    }
}

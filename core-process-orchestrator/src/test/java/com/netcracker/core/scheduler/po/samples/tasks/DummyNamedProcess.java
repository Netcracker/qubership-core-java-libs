package org.qubership.core.scheduler.po.samples.tasks;

import org.qubership.core.scheduler.po.ProcessDefinition;
import org.qubership.core.scheduler.po.task.NamedTask;

public class DummyNamedProcess extends ProcessDefinition {
    public DummyNamedProcess() {
        super("WarmUp");
        addTask(new NamedTask(DummyTask.class, "GetState"));
        addTask(new NamedTask(DummyTask.class), "GetState");
    }
}

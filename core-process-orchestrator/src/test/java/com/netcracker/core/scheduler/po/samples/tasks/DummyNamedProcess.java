package com.netcracker.core.scheduler.po.samples.tasks;

import com.netcracker.core.scheduler.po.ProcessDefinition;
import com.netcracker.core.scheduler.po.task.NamedTask;

public class DummyNamedProcess extends ProcessDefinition {
    public DummyNamedProcess() {
        super("WarmUp");
        addTask(new NamedTask(DummyTask.class, "GetState"));
        addTask(new NamedTask(DummyTask.class), "GetState");
    }
}

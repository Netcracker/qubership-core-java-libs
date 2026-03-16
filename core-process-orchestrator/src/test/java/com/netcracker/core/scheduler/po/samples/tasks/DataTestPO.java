package com.netcracker.core.scheduler.po.samples.tasks;

import com.netcracker.core.scheduler.po.ProcessDefinition;

public class DataTestPO extends ProcessDefinition {
    public DataTestPO() {
        super("MyData");
        addTask(DataTask.class);
    }
}

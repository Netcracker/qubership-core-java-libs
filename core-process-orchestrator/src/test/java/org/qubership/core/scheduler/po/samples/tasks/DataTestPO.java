package org.qubership.core.scheduler.po.samples.tasks;

import org.qubership.core.scheduler.po.ProcessDefinition;

public class DataTestPO extends ProcessDefinition {
    public DataTestPO() {
        super("MyData");
        addTask(DataTask.class);
    }
}

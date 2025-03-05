package org.qubership.core.scheduler.po.samples;

import org.qubership.core.scheduler.po.ProcessDefinition;
import org.qubership.core.scheduler.po.samples.tasks.DummyTask;
import org.qubership.core.scheduler.po.samples.tasks.DummyTask2;
import org.qubership.core.scheduler.po.task.NamedTask;

public class DymmyProcess extends ProcessDefinition {
    public DymmyProcess() {

        super("WarmUp");
        this.addTask(DummyTask2.class)
                .addTask(DummyTask.class)
                .addTask(new NamedTask(DummyTask.class, "d1"), DummyTask2.class.getName())
                .addDepend(DummyTask.class, DummyTask2.class)
                .addTask(new NamedTask(DummyTask.class, "d2"), DummyTask2.class.getName());
    }

}

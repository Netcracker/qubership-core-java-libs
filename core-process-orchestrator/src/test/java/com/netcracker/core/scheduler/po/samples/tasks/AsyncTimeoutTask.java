package org.qubership.core.scheduler.po.samples.tasks;

public class AsyncTimeoutTask extends DummyAsyncTask {
    public AsyncTimeoutTask() {
        super(AsyncTimeoutTask.class.getName(), 100);
    }
}

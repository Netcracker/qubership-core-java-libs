package org.qubership.core.scheduler.po.runnable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;

public class TerminateRunnable implements Callable<Boolean> {

    private final Future<?> f;
    private final String taskId;
    private final static Logger log = LoggerFactory.getLogger(TerminateRunnable.class);

    public TerminateRunnable(Future<?> f, String taskId) {
        this.f = f;
        this.taskId = taskId;
    }

    @Override
    public Boolean call() throws Exception {
        log.debug("{}: Terminating, f: {}", taskId, f);
        if (f != null) {
            f.cancel(true);
            while (!f.isDone()) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ignored) {

                }
            }
            log.info("Task {} terminated", taskId);
        }
        return Boolean.TRUE;
    }
}

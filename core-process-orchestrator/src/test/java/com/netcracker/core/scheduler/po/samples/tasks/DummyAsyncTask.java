package com.netcracker.core.scheduler.po.samples.tasks;

import com.netcracker.core.scheduler.po.DataContext;
import com.netcracker.core.scheduler.po.context.TaskExecutionContext;
import com.netcracker.core.scheduler.po.task.templates.AsyncTaskWithPolling;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DummyAsyncTask extends AsyncTaskWithPolling {

    private final static Logger logger = LoggerFactory.getLogger(DummyAsyncTask.class);
    protected int count = 3;

    public DummyAsyncTask() {
        super(DummyAsyncTask.class.getName());
        polingInterval = 1;
    }

    public DummyAsyncTask(String name, int count) {
        super(name);
        this.count = count;
        polingInterval = 1;
    }

    @Override
    public void executeSyncPart(TaskExecutionContext instance) {
        logger.info("Sync Part");
        instance.getProcess().getContext().apply((DataContext c) -> c.put("Test", "1"));
    }

    @Override
    public boolean executePolling(TaskExecutionContext instance) {
        DataContext dataContext = instance.getTaskInstance().getContext();
        int counter = (Integer) dataContext.computeIfAbsent("CF", t -> Integer.parseInt("0"));
        logger.info("Pool {} times", counter);
        if (counter > count) return true;
        counter = counter + 1;
        dataContext.put("CF", counter);
        dataContext.save();
        return false;
    }

    @Override
    public void onPollingSuccess(TaskExecutionContext instance) {
        logger.info("Pooling success");
    }

    @Override
    public void onPollingFailed(TaskExecutionContext instance, Exception e) {

        logger.error("", e);

    }
}

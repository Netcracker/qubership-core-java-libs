package org.qubership.maas.declarative.kafka.client.impl.client;

import org.apache.commons.lang3.concurrent.BasicThreadFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

/*
* Used to keep common system thread for executing internal system library tasks
* NOTE: Don't use out of library
* */
public class MaasKafkaInternalScheduledSystemExecServiceKeeper {

    protected static final ThreadFactory sysExecThreadFactory = new BasicThreadFactory.Builder()
            .namingPattern("maas-kafka-sys-exec-%s")
            .daemon(true)
            .build();
    private static final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor(sysExecThreadFactory);


    public static ScheduledExecutorService getExecutorService() {
        return executorService;
    }
}

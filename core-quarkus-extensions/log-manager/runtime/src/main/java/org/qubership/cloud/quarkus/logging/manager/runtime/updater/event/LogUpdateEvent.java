package org.qubership.cloud.quarkus.logging.manager.runtime.updater.event;

import java.util.Map;

public class LogUpdateEvent extends AbstractCoreConsulEvent {

    public LogUpdateEvent(Map<String, String> properties, String root) {
        super(properties, root);
    }
}

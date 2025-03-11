package org.qubership.cloud.quarkus.logging.manager.runtime.updater.event;

import java.util.Map;

public class ConfigUpdatedEvent extends AbstractCoreConsulEvent{
    public ConfigUpdatedEvent(Map<String, String> properties, String root) {
        super(properties, root);
    }
}

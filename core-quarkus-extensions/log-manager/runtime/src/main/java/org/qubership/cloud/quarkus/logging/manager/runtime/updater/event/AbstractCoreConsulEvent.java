package org.qubership.cloud.quarkus.logging.manager.runtime.updater.event;

import java.util.Map;

abstract class AbstractCoreConsulEvent {

    private final Map<String, String> properties;
    private final String root;

    AbstractCoreConsulEvent(Map<String, String> properties, String root) {
        this.properties = properties;
        this.root = root;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public String getRoot() {
        return root;
    }
}

package org.qubership.cloud.consul.config.source.runtime;

import org.eclipse.microprofile.config.spi.ConfigSource;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

public class ConsulConfigSource implements ConfigSource {
    private final String name;
    private final AtomicReference<Map<String, String>> properties;

    public static final int PRIORITY = 500; // Higher than any priority mentioned in Quarkus doc

    public ConsulConfigSource(String name, AtomicReference<Map<String, String>> properties) {
        this.name = name;
        this.properties = properties;
    }

    @Override
    public Map<String, String> getProperties() {
        return Collections.unmodifiableMap(properties.get());
    }

    @Override
    public Set<String> getPropertyNames() {
        return properties.get().keySet();
    }

    @Override
    public String getValue(String propertyName) {
        return properties.get().get(propertyName);
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public int getOrdinal() {
        return PRIORITY;
    }
}

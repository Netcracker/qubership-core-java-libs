package org.qubership.cloud.dbaas.client.management;


import org.qubership.cloud.dbaas.client.management.classifier.DbaaSClassifierBuilder;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static org.qubership.cloud.dbaas.client.DbaasConst.*;

/**
 * This classifier is used as API type for dbaas-client's users and
 * abstracts from ConnectionSearchKey from MongoEvolution lib
 * <p>
 * Additionally this classifier can be extended with {@link Builder}
 * and can extend its uniqueness by adding additional properties
 */
public class DbaasDbClassifier implements ClassifierAsMap {
    private final Map<String, Object> params = new HashMap<>();

    public DbaasDbClassifier(Map<String, Object> extendedParams) {
        if (extendedParams != null) {
            this.params.putAll(extendedParams);
        }
    }

    public void putProperty(String name, Object value) {
        this.params.put(name, value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DbaasDbClassifier that = (DbaasDbClassifier) o;
        return Objects.equals(params, that.params);
    }

    @Override
    public int hashCode() {
        return Objects.hash(params);
    }

    @Override
    public String toString() {
        return String.valueOf(params);
    }

    @Override
    public Map<String, Object> asMap() {
        return Collections.unmodifiableMap(this.params);
    }

    public static class Builder implements DbaaSClassifierBuilder {
        private Map<String, Object> params = new HashMap<>();

        public Builder() {
        }

        public Builder withProperty(String name, Object value) {
            Objects.requireNonNull(name, "property's name cannot be null");
            this.params.put(name, value);
            return this;
        }

        public Builder withProperties(Map<String, Object> properties) {
            Objects.requireNonNull(properties, "properties name cannot be null");
            this.params.putAll(properties);
            return this;
        }

        public Builder withCustomKey(String key, Object value) {
            Objects.requireNonNull(key, "property name cannot be null");
            Objects.requireNonNull(value, "property value cannot be null");
            if (this.params.containsKey(CUSTOM_KEYS)) {
                ((Map<String, Object>) this.params.get(CUSTOM_KEYS)).put(key, value);
            } else {
                Map<String, Object> customKeys = new HashMap<>();
                customKeys.put(key, value);
                params.put(CUSTOM_KEYS, customKeys);
            }
            return this;
        }

        public DbaasDbClassifier build() {
            return new DbaasDbClassifier(params);
        }
    }
}

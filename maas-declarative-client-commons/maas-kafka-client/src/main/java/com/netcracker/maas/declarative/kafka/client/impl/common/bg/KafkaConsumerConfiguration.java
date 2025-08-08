package com.netcracker.maas.declarative.kafka.client.impl.common.bg;

import org.qubership.cloud.maas.bluegreen.kafka.ConsumerConsistencyMode;
import org.qubership.cloud.maas.bluegreen.kafka.OffsetSetupStrategy;

import java.util.Map;
import java.util.function.Supplier;

public class KafkaConsumerConfiguration {
    private final boolean blueGreen;
    private final Map<String, Object> configs;
    private final ConsumerConsistencyMode consumerConsistencyMode;
    private final OffsetSetupStrategy candidateOffsetShift;
    private final boolean filterEnabled;
    private final boolean localdev;
    private final boolean versioned;


    @Deprecated(since = "4.0.10")
    public KafkaConsumerConfiguration(boolean blueGreen,
                                      ConsumerConsistencyMode consumerConsistencyMode,
                                      OffsetSetupStrategy candidateOffsetShift,
                                      boolean filterEnabled,
                                      Map<String, Object> configs,
                                      Supplier<String> tokenSupplier,
                                      boolean localdev) {
        this(blueGreen, consumerConsistencyMode, candidateOffsetShift, filterEnabled, configs, localdev, false);
    }

    public KafkaConsumerConfiguration(boolean blueGreen,
                                      ConsumerConsistencyMode consumerConsistencyMode,
                                      OffsetSetupStrategy candidateOffsetShift,
                                      boolean filterEnabled,
                                      Map<String, Object> configs,
                                      boolean localdev,
                                      boolean versioned) {
        this.blueGreen = blueGreen;
        this.consumerConsistencyMode = consumerConsistencyMode;
        this.candidateOffsetShift = candidateOffsetShift;
        this.filterEnabled = filterEnabled;
        this.configs = configs;
        this.localdev = localdev;
        this.versioned = versioned;
    }

    public boolean isBlueGreen() {
        return blueGreen;
    }

    public Map<String, Object> getConfigs() {
        return configs;
    }

    public ConsumerConsistencyMode getConsumerConsistencyMode() {
        return consumerConsistencyMode;
    }

    public OffsetSetupStrategy getCandidateOffsetShift() {
        return candidateOffsetShift;
    }

    public boolean isFilterEnabled() {
        return filterEnabled;
    }

    @Deprecated(since = "4.0.10")
    public Supplier<String> getTokenSupplier() {
        return null;
    }

    public boolean isLocaldev() {
        return localdev;
    }

    public boolean isVersioned() {
        return versioned;
    }

    public static class Builder {
        private boolean blueGreen;
        private final Map<String, Object> configs;
        private ConsumerConsistencyMode consumerConsistencyMode;
        private OffsetSetupStrategy candidateOffsetShift;
        private boolean filterEnabled;
        private boolean localdev;
        private boolean versioned;


        private Builder(Map<String, Object> configs) {
            this.configs = configs;
        }

        public KafkaConsumerConfiguration.Builder setBlueGreen(boolean blueGreen) {
            this.blueGreen = blueGreen;
            return this;
        }

        public KafkaConsumerConfiguration.Builder setConsumerConsistencyMode(ConsumerConsistencyMode consumerConsistencyMode) {
            this.consumerConsistencyMode = consumerConsistencyMode;
            return this;
        }

        public KafkaConsumerConfiguration.Builder setCandidateOffsetShift(OffsetSetupStrategy candidateOffsetShift) {
            this.candidateOffsetShift = candidateOffsetShift;
            return this;
        }

        public KafkaConsumerConfiguration.Builder setFilterEnabled(boolean filterEnabled) {
            this.filterEnabled = filterEnabled;
            return this;
        }

        @Deprecated(since = "4.0.10") // tokenSupplier is not required for bg kafka consumer
        public KafkaConsumerConfiguration.Builder setTokenSupplier(Supplier<String> tokenSupplier) {
            return this;
        }

        public KafkaConsumerConfiguration.Builder setLocaldev(boolean localdev) {
            this.localdev = localdev;
            return this;
        }

        public KafkaConsumerConfiguration.Builder setVersioned(boolean versioned) {
            this.versioned = versioned;
            return this;
        }

        public KafkaConsumerConfiguration build() {
            return new KafkaConsumerConfiguration(
                    blueGreen,
                    consumerConsistencyMode,
                    candidateOffsetShift,
                    filterEnabled,
                    configs,
                    localdev,
                    versioned
            );
        }
    }

    public static KafkaConsumerConfiguration.Builder builder(Map<String, Object> configs) {
        return new KafkaConsumerConfiguration.Builder(configs);
    }
}

package com.netcracker.maas.declarative.kafka.client.api.model.definition;

import com.netcracker.cloud.maas.bluegreen.kafka.ConsumerConsistencyMode;
import com.netcracker.cloud.maas.bluegreen.kafka.OffsetSetupStrategy;

import java.util.function.Supplier;

public class MaasKafkaBlueGreenDefinition {
    private final boolean enabled;
    private final ConsumerConsistencyMode consumerConsistencyMode;
    private final OffsetSetupStrategy candidateOffsetShift;
    private final boolean filterEnabled;
    private final boolean localdev;

    @Deprecated(since = "4.0.10")
    public MaasKafkaBlueGreenDefinition(boolean enabled,
                                        ConsumerConsistencyMode consumerConsistencyMode,
                                        OffsetSetupStrategy candidateOffsetShift,
                                        boolean filterEnabled,
                                        Supplier<String> tokenSupplier,
                                        boolean localdev) {
        this(enabled, consumerConsistencyMode, candidateOffsetShift, filterEnabled, localdev);
    }

    public MaasKafkaBlueGreenDefinition(boolean enabled,
                                        ConsumerConsistencyMode consumerConsistencyMode,
                                        OffsetSetupStrategy candidateOffsetShift,
                                        boolean filterEnabled,
                                        boolean localdev) {
        this.enabled = enabled;
        this.consumerConsistencyMode = consumerConsistencyMode;
        this.candidateOffsetShift = candidateOffsetShift;
        this.filterEnabled = filterEnabled;
        this.localdev = localdev;
    }


    public MaasKafkaBlueGreenDefinition(boolean enabled,
                                        ConsumerConsistencyMode consumerConsistencyMode,
                                        OffsetSetupStrategy candidateOffsetShift,
                                        boolean filterEnabled) {
        this(enabled, consumerConsistencyMode, candidateOffsetShift, filterEnabled, false);
    }

    public boolean isEnabled() {
        return enabled;
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
}

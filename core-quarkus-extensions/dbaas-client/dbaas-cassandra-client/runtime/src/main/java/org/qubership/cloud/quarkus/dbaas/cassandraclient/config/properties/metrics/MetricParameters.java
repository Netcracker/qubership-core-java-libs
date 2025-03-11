package org.qubership.cloud.quarkus.dbaas.cassandraclient.config.properties.metrics;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

@ConfigGroup
@Getter
@Accessors(fluent = true)
public class MetricParameters {
    /**
     * The largest latency that we expect to record.
     * <p>
     * This should be slightly higher than request.timeout (in theory, readings can't be higher
     * than the timeout, but there might be a small overhead due to internal scheduling).
     * <p>
     * This is used to scale internal data structures. If a higher recording is encountered at
     * runtime, it is discarded and a warning is logged.
     */
    @ConfigItem
    Optional<Duration> highestLatency;

    /**
     * The shortest latency that we expect to record. This is used to scale internal data structures.
     */
    @ConfigItem
    Optional<Duration> lowestLatency;

    /**
     * The number of significant decimal digits to which internal structures will maintain
     * value resolution and separation (for example, 3 means that recordings up to 1 second
     * will be recorded with a resolution of 1 millisecond or better).
     */
    @ConfigItem
    OptionalInt significantDigits;

    /**
     * The interval at which percentile data is refreshed.
     */
    @ConfigItem
    Optional<Duration> refreshInterval;

    /**
     * An optional list of latencies to track as part of the application's service-level objectives (SLOs).
     */
    @ConfigItem
    Optional<List<Duration>> slo;
}

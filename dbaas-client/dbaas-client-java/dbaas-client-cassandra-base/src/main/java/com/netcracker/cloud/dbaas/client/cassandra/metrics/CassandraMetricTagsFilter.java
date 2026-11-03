package com.netcracker.cloud.dbaas.client.cassandra.metrics;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.config.MeterFilterReply;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Keeps a cassandra meter out of the Prometheus registry when its name is already registered there with other tag keys.
 * <p>
 * Prometheus in Quarkus 3.34+ throws IllegalArgumentException in this case, and the tags depend on the classifier. The driver
 * registers meters while it creates a session, so the exception would fail the session creation, or leave it hanging forever
 * for node metrics. Denying one meter in one registry leaves other meters and other registries working.
 */
@Slf4j
public class CassandraMetricTagsFilter implements MeterFilter {

    // The newer io.micrometer.prometheusmetrics registry accepts different tag keys since Micrometer 1.17, so it must keep them
    private static final String STRICT_REGISTRY_CLASS = "io.micrometer.prometheus.PrometheusMeterRegistry";
    private static final String METRIC_PREFIX = "cassandra.";
    private static final Set<MeterRegistry> FILTERED_REGISTRIES = Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));

    private final MeterRegistry registry;
    private final Set<String> reportedMeters = ConcurrentHashMap.newKeySet();

    private CassandraMetricTagsFilter(MeterRegistry registry) {
        this.registry = registry;
    }

    public static void applyTo(MeterRegistry registry) {
        if (registry instanceof CompositeMeterRegistry composite) {
            composite.getRegistries().forEach(CassandraMetricTagsFilter::applyTo);
        } else if (STRICT_REGISTRY_CLASS.equals(registry.getClass().getName()) && FILTERED_REGISTRIES.add(registry)) {
            registry.config().meterFilter(new CassandraMetricTagsFilter(registry));
        }
    }

    @Override
    public MeterFilterReply accept(Meter.Id id) {
        if (!id.getName().startsWith(METRIC_PREFIX)) {
            return MeterFilterReply.NEUTRAL;
        }
        Set<String> tagKeys = tagKeys(id);
        Optional<Set<String>> otherTagKeys = registry.find(id.getName()).meters().stream()
                .map(meter -> tagKeys(meter.getId()))
                .filter(keys -> !keys.equals(tagKeys))
                .findFirst();
        if (otherTagKeys.isEmpty()) {
            return MeterFilterReply.NEUTRAL;
        }
        if (reportedMeters.add(id.getName() + tagKeys)) {
            log.error("Metric {} with tag keys {} is not registered in Prometheus: it already has tag keys {}",
                    id.getName(), tagKeys, otherTagKeys.get());
        }
        return MeterFilterReply.DENY;
    }

    private static Set<String> tagKeys(Meter.Id id) {
        return id.getTags().stream().map(Tag::getKey).collect(Collectors.toSet());
    }
}

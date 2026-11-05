package com.netcracker.cloud.dbaas.client.metrics;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.config.MeterFilterReply;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Keeps a DBaaS client meter out of the Prometheus registry when its name is already registered there with other tag keys.
 * <p>
 * Tags of these meters depend on the database classifier, and Prometheus in Quarkus 3.34+ throws IllegalArgumentException
 * on different tag keys. Denying one meter in one registry leaves other meters and other registries working.
 */
@Slf4j
public class DbaasMetricTagsFilter implements MeterFilter {

    // The newer io.micrometer.prometheusmetrics registry accepts different tag keys since Micrometer 1.17, so it must keep them
    private static final String STRICT_REGISTRY_CLASS = "io.micrometer.prometheus.PrometheusMeterRegistry";
    private static final List<String> METRIC_PREFIXES = List.of("agroal.", "cassandra.", "opensearch_client_requests_seconds");
    private static final Set<MeterRegistry> FILTERED_REGISTRIES = Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));

    private final MeterRegistry registry;
    private final Set<String> reportedMeters = ConcurrentHashMap.newKeySet();

    private DbaasMetricTagsFilter(MeterRegistry registry) {
        this.registry = registry;
    }

    public static void applyTo(MeterRegistry registry) {
        if (registry instanceof CompositeMeterRegistry composite) {
            composite.getRegistries().forEach(DbaasMetricTagsFilter::applyTo);
            return;
        }
        if (!STRICT_REGISTRY_CLASS.equals(registry.getClass().getName())) {
            return;
        }
        synchronized (FILTERED_REGISTRIES) {
            if (FILTERED_REGISTRIES.add(registry)) {
                registry.config().meterFilter(new DbaasMetricTagsFilter(registry));
            }
        }
    }

    @Override
    public MeterFilterReply accept(Meter.Id id) {
        if (METRIC_PREFIXES.stream().noneMatch(id.getName()::startsWith)) {
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

package org.qubership.cloud.frameworkextensions.monitoring.health.indicator;

import org.qubership.cloud.frameworkextensions.monitoring.health.HealthStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@ConditionalOnProperty(prefix = "health.memory.heap", name = "enabled", havingValue = "true")
@Component
public class HeapHealthIndicator implements HealthIndicator {
    @Value("${health.memory.heap.threshold:90}")
    long threshold;

    @Value("${health.memory.heap.warning:75}")
    long warning;

    @Override
    public Health health() {
        long limit = Runtime.getRuntime().maxMemory();
        long free = Runtime.getRuntime().freeMemory();
        long usage = 100 - (free * 100 / limit);
        log.debug("Heap usage - {}%", usage);

        Health.Builder status = Health.up();
        if (usage >= threshold) {
            status.outOfService();
        } else if (usage >= warning) {
            status.status(HealthStatus.WARNING.name());
        }

        return status.build();
    }
}

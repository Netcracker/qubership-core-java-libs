package com.netcracker.cloud.frameworkextensions.monitoring.health.indicator;

import lombok.extern.slf4j.Slf4j;
import com.netcracker.cloud.frameworkextensions.monitoring.health.HealthStatus;
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

    private final MemoryInfo memoryInfo;

    public HeapHealthIndicator() {
        this.memoryInfo = new RuntimeMemoryInfo();
    }

    /**
     * Constructor for testing purposes only
     */
    HeapHealthIndicator(MemoryInfo memoryInfo) {
        this.memoryInfo = memoryInfo;
    }

    protected long calculateHeapUsage() {
        long limit = memoryInfo.getMaxMemory();
        long free = memoryInfo.getFreeMemory();
        return 100 - (free * 100 / limit);
    }

    @Override
    public Health health() {
        long usage = calculateHeapUsage();
        log.debug("Heap usage - {}%", usage);

        Health.Builder status = Health.up();
        if (usage >= threshold) {
            status.outOfService();
        } else if (usage >= warning) {
            status.status(HealthStatus.WARNING.name());
        }

        return status.build();
    }

    interface MemoryInfo {
        long getMaxMemory();
        long getFreeMemory();
    }

    private static class RuntimeMemoryInfo implements MemoryInfo {
        private final Runtime runtime;

        RuntimeMemoryInfo() {
            this.runtime = Runtime.getRuntime();
        }

        @Override
        public long getMaxMemory() {
            return runtime.maxMemory();
        }

        @Override
        public long getFreeMemory() {
            return runtime.freeMemory();
        }
    }
}

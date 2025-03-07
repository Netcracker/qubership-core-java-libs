package org.qubership.cloud.frameworkextensions.monitoring.health.indicator;

import org.qubership.cloud.frameworkextensions.monitoring.health.HealthStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;

@Slf4j
public abstract class MemoryHealthIndicator implements HealthIndicator {
    String beanName;
    long threshold;
    long warning;

    public MemoryHealthIndicator(String beanName, long threshold, long warning) {
        this.beanName = beanName;
        this.threshold = threshold;
        this.warning = warning;
    }

    @Override
    public Health health() {
        Health.Builder status = Health.up();

        for (MemoryPoolMXBean memoryMXBean : ManagementFactory.getMemoryPoolMXBeans()) {
            if (memoryMXBean.getName().equals(beanName)) {
                long usage = getUsage(memoryMXBean);

                if (usage >= threshold) {
                    status.outOfService();
                } else if (usage >= warning) {
                    status.status(HealthStatus.WARNING.name());
                }

                break;
            }
        }

        return status.build();
    }

    private long getUsage(MemoryPoolMXBean poolMXBean) {
        long current = poolMXBean.getUsage().getUsed();
        long max = poolMXBean.getUsage().getMax();
        long usage = current * 100 / max;
        log.debug("{} pool usage - {}%", beanName, usage);
        return usage;
    }
}

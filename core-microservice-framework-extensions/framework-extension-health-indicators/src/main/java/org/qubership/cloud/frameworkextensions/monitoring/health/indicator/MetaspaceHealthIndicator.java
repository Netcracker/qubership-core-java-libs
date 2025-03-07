package org.qubership.cloud.frameworkextensions.monitoring.health.indicator;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@ConditionalOnProperty(prefix = "health.memory.metaspace", name = "enabled", havingValue = "true")
@Component
public class MetaspaceHealthIndicator extends MemoryHealthIndicator {
    public MetaspaceHealthIndicator(
            @Value("${health.memory.metaspace.threshold:90}") long threshold,
            @Value("${health.memory.metaspace.warning:75}") long warning
    ) {
        super("Metaspace", threshold, warning);
    }
}

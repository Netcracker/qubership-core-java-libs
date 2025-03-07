package org.qubership.cloud.frameworkextensions.monitoring.health.indicator;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@ConditionalOnProperty(prefix = "health.memory.compressed-class-space", name = "enabled", havingValue = "true")
@Component
public class CompressedClassSpaceHealthIndicator extends MemoryHealthIndicator {
    public CompressedClassSpaceHealthIndicator(
            @Value("${health.memory.compressed-class-space.threshold:90}") long threshold,
            @Value("${health.memory.compressed-class-space.warning:75}") long warning
    ) {
        super("Compressed Class Space", threshold, warning);
    }
}

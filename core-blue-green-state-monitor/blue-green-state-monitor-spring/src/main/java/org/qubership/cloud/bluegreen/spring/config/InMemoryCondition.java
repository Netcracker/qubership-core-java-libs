package org.qubership.cloud.bluegreen.spring.config;

import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

public class InMemoryCondition extends AnyNestedCondition {
    public InMemoryCondition() {
        super(ConfigurationPhase.PARSE_CONFIGURATION);
    }

    @ConditionalOnProperty(value = "blue-green.enabled", havingValue = "false")
    static class OnBlueGreenDisabledProperty {
    }

    @ConditionalOnProperty(value = "blue-green.state-monitor.dev.enabled", havingValue = "true")
    static class OnDevProperty {
    }
}

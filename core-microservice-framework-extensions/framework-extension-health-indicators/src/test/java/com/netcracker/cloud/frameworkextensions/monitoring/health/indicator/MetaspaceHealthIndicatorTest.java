package com.netcracker.cloud.frameworkextensions.monitoring.health.indicator;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class MetaspaceHealthIndicatorTest {

    private static final String METASPACE_POOL_NAME = "Metaspace";
    private static final long DEFAULT_THRESHOLD = 90;
    private static final long DEFAULT_WARNING = 75;

    @Test
    void testDefaultConstructor() {
        // Given
        MetaspaceHealthIndicator healthIndicator = new MetaspaceHealthIndicator(DEFAULT_THRESHOLD, DEFAULT_WARNING);

        // Then
        assertEquals(METASPACE_POOL_NAME, healthIndicator.beanName);
        assertEquals(DEFAULT_THRESHOLD, healthIndicator.threshold);
        assertEquals(DEFAULT_WARNING, healthIndicator.warning);
    }
}

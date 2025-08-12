package com.netcracker.cloud.frameworkextensions.monitoring.health.indicator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HeapHealthIndicatorTest {

    private static final long DEFAULT_THRESHOLD = 90;
    private static final long DEFAULT_WARNING = 75;

    @Mock
    private HeapHealthIndicator.MemoryInfo memoryInfo;

    private HeapHealthIndicator healthIndicator;

    @BeforeEach
    void setUp() {
        healthIndicator = new HeapHealthIndicator(memoryInfo);
        healthIndicator.threshold = DEFAULT_THRESHOLD;
        healthIndicator.warning = DEFAULT_WARNING;
    }

    @ParameterizedTest
    @CsvSource({
            "1000, 200, UP",
            "1000, 500, UP",
            "1000, 750, WARNING",
            "1000, 800, WARNING",
            "1000, 900, OUT_OF_SERVICE",
            "1000, 950, OUT_OF_SERVICE",
            "1000, 1000, OUT_OF_SERVICE",
    })
    void testHealth(String maxMemory, String currentMemory, String expected ) {
        when(memoryInfo.getMaxMemory()).thenReturn(Long.parseLong(maxMemory));
        when(memoryInfo.getFreeMemory()).thenReturn(Long.parseLong(maxMemory) - Long.parseLong(currentMemory));

        Health health = healthIndicator.health();

        assertEquals(expected, health.getStatus().toString());
    }
}

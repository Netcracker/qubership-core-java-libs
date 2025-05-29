package org.qubership.cloud.frameworkextensions.monitoring.health.indicator;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemoryHealthIndicatorTest {

    private static final String TEST_BEAN_NAME = "TestPool";
    private static final long DEFAULT_THRESHOLD = 90;
    private static final long DEFAULT_WARNING = 75;

    @Mock
    private MemoryPoolMXBean memoryPoolMXBean;

    @Mock
    private MemoryUsage memoryUsage;

    private MemoryHealthIndicator healthIndicator;
    private MockedStatic<ManagementFactory> managementFactory;

    @BeforeEach
    void setUp() {
        healthIndicator = new MemoryHealthIndicator(TEST_BEAN_NAME, DEFAULT_THRESHOLD, DEFAULT_WARNING) {};
        managementFactory = mockStatic(ManagementFactory.class);
        managementFactory.when(ManagementFactory::getMemoryPoolMXBeans)
                .thenReturn(Collections.singletonList(memoryPoolMXBean));

        when(memoryPoolMXBean.getName()).thenReturn(TEST_BEAN_NAME);
        when(memoryPoolMXBean.getUsage()).thenReturn(memoryUsage);
    }

    @AfterEach
    void tearDown() {
        managementFactory.close();
    }

    @ParameterizedTest
    @CsvSource({
            "1000, 0, UP",
            "1000, 200, UP",
            "1000, 500, UP",
            "1000, 750, WARNING",
            "1000, 800, WARNING",
            "1000, 900, OUT_OF_SERVICE",
            "1000, 950, OUT_OF_SERVICE",
            "1000, 1000, OUT_OF_SERVICE",
    })
    void testHealth(String maxMemory, String currentMemory, String expected) {
        // Given
        when(memoryUsage.getMax()).thenReturn(Long.parseLong(maxMemory));
        when(memoryUsage.getUsed()).thenReturn(Long.parseLong(currentMemory));

        // When
        Health health = healthIndicator.health();

        // Then
        assertEquals(expected, health.getStatus().toString());
    }
}

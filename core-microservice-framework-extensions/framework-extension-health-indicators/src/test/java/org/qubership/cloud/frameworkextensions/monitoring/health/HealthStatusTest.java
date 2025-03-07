package org.qubership.cloud.frameworkextensions.monitoring.health;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class HealthStatusTest {

    @Test
    public void healthStatusIsOkTest() {
        assertTrue(HealthStatus.isOk(HealthStatus.WARNING.name()));
        assertTrue(HealthStatus.isOk(HealthStatus.UNKNOWN.name()));
        assertTrue(HealthStatus.isOk(HealthStatus.UP.name()));
        assertFalse(HealthStatus.isOk(HealthStatus.DOWN.name()));
        assertFalse(HealthStatus.isOk(HealthStatus.OUT_OF_SERVICE.name()));
        assertFalse(HealthStatus.isOk(HealthStatus.PROBLEM.name()));
    }
}

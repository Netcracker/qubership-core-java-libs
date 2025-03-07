package org.qubership.cloud.frameworkextensions.monitoring.health;

import lombok.Data;

public enum HealthStatus {
    DOWN, OUT_OF_SERVICE, PROBLEM, WARNING, UNKNOWN, UP;

    public static boolean isOk(final String healthStatus) {
        return WARNING.name().equals(healthStatus) ||
                UNKNOWN.name().equals(healthStatus) ||
                UP.name().equals(healthStatus);
    }

    @Data
    public static class MicroserviceHealthStatus {
        private String status;
    }
}

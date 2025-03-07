package org.qubership.cloud.log.manager.common.logback;

import org.qubership.cloud.log.manager.common.LoggingSystem;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class LogbackLoggingSystemProviderTest {
    @Test
    void testProvideWhenLogbackIsPresent() {
        LogbackLoggingSystemProvider provider = new LogbackLoggingSystemProvider();
        LoggingSystem loggingSystem = provider.provide();
        assertNotNull(loggingSystem, "Expected LogbackLoggingSystem to be provided when Logback is present.");
    }

    @Test
    void testProvideWhenLogbackNotPresent() {
        LogbackLoggingSystemProvider provider = new LogbackLoggingSystemProvider(false);
        LoggingSystem loggingSystem = provider.provide();
        assertNull(loggingSystem);
    }

}

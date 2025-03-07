package org.qubership.cloud.log.manager.common.log4j2;

import org.qubership.cloud.log.manager.common.LoggingSystem;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class Log4j2LoggingSystemProviderTest {
    @Test
    void testProvideWhenLog4j2IsPresent() {
        Log4j2LoggingSystemProvider provider = new Log4j2LoggingSystemProvider();
        LoggingSystem loggingSystem = provider.provide();
        assertNotNull(loggingSystem);
    }

    @Test
    void testProvideWhenLog4j2NotPresent() {
        Log4j2LoggingSystemProvider provider = new Log4j2LoggingSystemProvider(false);
        LoggingSystem loggingSystem = provider.provide();
        assertNull(loggingSystem);
    }
}

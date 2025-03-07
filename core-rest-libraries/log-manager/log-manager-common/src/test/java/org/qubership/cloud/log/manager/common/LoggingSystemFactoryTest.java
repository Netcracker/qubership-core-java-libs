package org.qubership.cloud.log.manager.common;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LoggingSystemFactoryTest {

    @Test
    void testLoggingSystemProvidersInitialization() {
        assertEquals(3, LoggingSystemFactory.loggingSystemProviders.size(), "Expected 3 logging system providers to be initialized.");
    }

    @Test
    void testGetLoggingSystems() {
        List<LoggingSystem> loggingSystems = LoggingSystemFactory.get();
        assertEquals(3, loggingSystems.size(), "Expected 3 logging systems to be created.");
    }
}

package org.qubership.cloud.quarkus.logging.manager.runtime;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LogControllerTest {
    @Test
    void testGetLoggers() {
        Map<String, String> jsonObjectBuilder = LogController.getLoggers();
        assertEquals("INFO", jsonObjectBuilder.get(""));
    }
}

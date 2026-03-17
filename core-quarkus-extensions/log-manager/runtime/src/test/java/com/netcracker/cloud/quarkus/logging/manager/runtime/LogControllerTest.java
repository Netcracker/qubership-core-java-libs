package com.netcracker.cloud.quarkus.logging.manager.runtime;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Disabled //TODO need to fix - failed in monorepo
class LogControllerTest {
    @Test
    void testGetLoggers() {
        Map<String, String> jsonObjectBuilder = LogController.getLoggers();
        assertEquals("INFO", jsonObjectBuilder.get(""));
    }
}

package com.netcracker.cloud.quarkus.logging.manager.runtime;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LogControllerTest {

    private Level rootLevelBeforeTest;

    @BeforeEach
    void setUp() {
        Logger root = Logger.getLogger("");
        rootLevelBeforeTest = root.getLevel();
        root.setLevel(Level.INFO);
    }

    @AfterEach
    void tearDown() {
        Logger.getLogger("").setLevel(rootLevelBeforeTest);
    }

    @Test
    void testGetLoggers() {
        Map<String, String> jsonObjectBuilder = LogController.getLoggers();
        assertEquals("INFO", jsonObjectBuilder.get(""));
    }
}

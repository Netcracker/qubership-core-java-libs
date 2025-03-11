package org.qubership.cloud.quarkus.logging.manager.runtime.updater;

import org.qubership.cloud.quarkus.logging.manager.runtime.updater.event.ConfigUpdatedEvent;
import org.qubership.cloud.quarkus.logging.manager.runtime.updater.event.LogUpdateEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


class LoggerUpdaterTest {

    private static LoggerUpdater loggerUpdater;

    @BeforeEach
    void beforeEach() {
        loggerUpdater = spy(LoggerUpdater.class);
    }

    @Test
    void onConfigUpdated() {
        Map<String, String> config = new HashMap<>();
        config.put("quarkus.log.level", Level.FINE.getName().toLowerCase());
        config.put("quarkus.log.category.\"org.hibernate\".level", Level.INFO.getName());
        config.put("quarkus.log.category.\"null.lvl\".level", null);
        config.put("quarkus.log.category.\"empty.lvl\".level", "");


        Logger root = Logger.getLogger("");
        root.setLevel(Level.OFF);

        Logger hibernateLogger = Logger.getLogger("org.hibernate");
        hibernateLogger.setLevel(Level.OFF);

        loggerUpdater.onConfigUpdated(new ConfigUpdatedEvent(config, ""));

        assertEquals(Level.FINE, root.getLevel());
        assertEquals(Level.INFO, hibernateLogger.getLevel());
    }

    @Test
    void onConfigUpdated_loggingLevelPrefix() {
        Map<String, String> config = new HashMap<>();
        config.put("logging.level.com.example.cloud", "trace");

        loggerUpdater.onConfigUpdated(new ConfigUpdatedEvent(config, ""));

        assertEquals("TRACE", Logger.getLogger("com.example.cloud").getLevel().getName());
    }

    @Test
    void onConfigUpdated_WrongLvlName() {
        Map<String, String> config = new HashMap<>();
        config.put("quarkus.log.category.\"lvl.wrong\".level", "my-awesome-lvl");

        assertThrows(RuntimeException.class, () -> loggerUpdater.onConfigUpdated(new ConfigUpdatedEvent(config, "")));
    }

    @Test
    void mustTryToChangeAllLevels_onErrorOfOne() {
        Map<String, String> config = new TreeMap<>();
        config.put("quarkus.log.category.\"io.vertx.core.impl.ContextImpl\".level", "my-awesome-lvl");
        config.put("quarkus.log.category.\"io.vertx.core.impl.ContextImplLexicographicallyHigher\".level", "INFO");

        Logger problematicLogger = Logger.getLogger("io.vertx.core.impl.ContextImpl");
        problematicLogger.setLevel(Level.OFF);

        Logger fineLogger = Logger.getLogger("io.vertx.core.impl.ContextImplLexicographicallyHigher");
        fineLogger.setLevel(Level.OFF);

        assertThrows(RuntimeException.class, () -> loggerUpdater.onConfigUpdated(new ConfigUpdatedEvent(config, "")));

        assertEquals(Level.INFO, fineLogger.getLevel());
        assertEquals(Level.OFF, problematicLogger.getLevel());
    }

    @Test
    void doNotChangeLoggerUpdaterLogLvl() {
        Map<String, String> config = new HashMap<>();

        Logger updaterLogger = Logger.getLogger("io.vertx.core.impl.ContextImpl");
        updaterLogger.setLevel(Level.OFF);

        loggerUpdater.onConfigUpdated(new ConfigUpdatedEvent(config, ""));

        assertEquals(Level.OFF, updaterLogger.getLevel());
    }

    @Test
    void testOnLogUpdateWhenPropertiesAreNull() {
        LogUpdateEvent logUpdateEvent = new LogUpdateEvent(null, null);

        loggerUpdater.onLogUpdate(logUpdateEvent);

        // Verify that updateLogLevel is never called
        verify(loggerUpdater, never()).updateLogLevel(anyString(), anyString());
    }

    @Test
    void testOnLogUpdateWhenPropertiesAreEmpty() {
        LogUpdateEvent logUpdateEvent = new LogUpdateEvent(Collections.emptyMap(), null);

        loggerUpdater.onLogUpdate(logUpdateEvent);

        // Verify that updateLogLevel is never called
        verify(loggerUpdater, never()).updateLogLevel(anyString(), anyString());
    }

    @Test
    void testOnLogUpdateWhenPropertiesContainLogLevelsWithCorrectPrefix() {
        Map<String, String> properties = new HashMap<>();
        properties.put("logging.level.com.example", "DEBUG");
        properties.put("com.cloud", "DEBUG");
        LogUpdateEvent logUpdateEvent = new LogUpdateEvent(properties, null);

        loggerUpdater.onLogUpdate(logUpdateEvent);

        verify(loggerUpdater, times(1)).updateLogLevel("logging.level.com.example", "DEBUG");
    }

    @Test
    void testOnLogUpdateWhenPropertiesContainLogLevelsWithoutCorrectPrefix() {
        Map<String, String> properties = new HashMap<>();
        properties.put("some.other.property", "INFO");
        LogUpdateEvent logUpdateEvent = new LogUpdateEvent(properties, null);

        loggerUpdater.onLogUpdate(logUpdateEvent);

        verify(loggerUpdater, never()).updateLogLevel(anyString(), anyString());
    }

    @Test
    void testUpdateLogLevelRootLevel() {
        loggerUpdater.updateLogLevel("logging.level.root", "TRACE");

        assertEquals("TRACE", Logger.getLogger("").getLevel().getName());
    }

    @Test
    void testUpdateLogLevel() {
        loggerUpdater.updateLogLevel("logging.level.org.qubership.test", "TRACE");

        assertEquals("TRACE", Logger.getLogger("org.qubership.test").getLevel().getName());
    }

    @Test
    void testLogLevelSnapshot() {
        Logger logger = Logger.getLogger("org.qubership.cloud");
        logger.setLevel(Level.parse("DEBUG"));
        assertEquals("DEBUG", logger.getLevel().getName());

        Map<String, String> config = new HashMap<>();
        config.put("logging.level.org.qubership.cloud", "trace");

        loggerUpdater.onConfigUpdated(new ConfigUpdatedEvent(config, ""));

        Map<String, String> logLevelSnapshot = loggerUpdater.getLogLevelSnapshot();
        assertEquals("DEBUG", logLevelSnapshot.get("logging.level.org.qubership.cloud"));
        assertEquals("TRACE", logger.getLevel().getName());

        // return log to previous level
        loggerUpdater.onConfigUpdated(new ConfigUpdatedEvent(Map.of(), ""));

        logLevelSnapshot = loggerUpdater.getLogLevelSnapshot();
        assertFalse(logLevelSnapshot.containsKey("logging.level.org.qubership.cloud"));
        assertEquals("DEBUG", logger.getLevel().getName());
    }

    @Test
    void testLogLevelSnapshot_onDeleteEvent() {
        Logger logger = Logger.getLogger("com.example");
        logger.setLevel(Level.parse("DEBUG"));
        assertEquals("DEBUG", logger.getLevel().getName());
        Map<String, String> config = new HashMap<>();
        config.put("logging.level.org.qubership.cloud", "TRACE");
        config.put("logging.level.com.example", "TRACE");

        loggerUpdater.onConfigUpdated(new ConfigUpdatedEvent(config,""));

        verify(loggerUpdater).updateLogLevel("logging.level.org.qubership.cloud", "TRACE");
        verify(loggerUpdater).updateLogLevel("logging.level.com.example", "TRACE");

        config.remove("logging.level.com.example");
        loggerUpdater.onConfigUpdated(new ConfigUpdatedEvent(config, ""));
        verify(loggerUpdater, times(2)).updateLogLevel("logging.level.org.qubership.cloud", "TRACE");
        verify(loggerUpdater).updateLogLevel("logging.level.com.example", "DEBUG"); // return to the previous level

        Map<String, String> logLevelSnapshot = loggerUpdater.getLogLevelSnapshot();
        assertEquals(1, logLevelSnapshot.size());
    }
}
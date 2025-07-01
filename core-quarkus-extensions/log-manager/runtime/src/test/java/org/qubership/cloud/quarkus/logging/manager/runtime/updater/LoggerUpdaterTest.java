package org.qubership.cloud.quarkus.logging.manager.runtime.updater;

import org.qubership.cloud.quarkus.logging.manager.runtime.updater.event.ConfigUpdatedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


class LoggerUpdaterTest {

    static String quarkusCategoryLevelTemplate = "quarkus.log.category.\"%s\".level";
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
        config.put("quarkus.log.category.\"org.qubership.cloud.consul.config.source.runtime.updater.loglvl.LoggerUpdater\".level", "ERROR");

        Logger updaterLogger = Logger.getLogger("io.vertx.core.impl.ContextImpl");
        updaterLogger.setLevel(Level.OFF);

        loggerUpdater.onConfigUpdated(new ConfigUpdatedEvent(config, ""));

        assertEquals(Level.OFF, updaterLogger.getLevel());
    }

    @Test
    void testUpdateLogLevel() {
        Logger logger = Logger.getLogger("org.qubership.cloud");
        loggerUpdater.updateLogLevel(String.format(quarkusCategoryLevelTemplate, "org.qubership.cloud"), "TRACE");

        assertEquals("TRACE", logger.getLevel().getName());
    }

    @Test
    void testLogLevelSnapshot() {
        Logger logger = Logger.getLogger("org.qubership.cloud");
        logger.setLevel(Level.parse("DEBUG"));
        assertEquals("DEBUG", logger.getLevel().getName());

        Map<String, String> config = new HashMap<>();
        config.put("quarkus.log.category.\"org.qubership.cloud\".level", "trace");

        loggerUpdater.onConfigUpdated(new ConfigUpdatedEvent(config, ""));

        Map<String, String> logLevelSnapshot = loggerUpdater.getLogLevelSnapshot();
        assertEquals("DEBUG", logLevelSnapshot.get("quarkus.log.category.\"org.qubership.cloud\".level"));
        assertEquals("TRACE", logger.getLevel().getName());

        // return log to the previous level
        loggerUpdater.onConfigUpdated(new ConfigUpdatedEvent(Map.of(), ""));

        logLevelSnapshot = loggerUpdater.getLogLevelSnapshot();
        assertFalse(logLevelSnapshot.containsKey("quarkus.log.category.\"org.qubership.cloud\".level"));
        assertEquals("DEBUG", logger.getLevel().getName());
    }

    @Test
    void testLogLevelSnapshot_onDeleteEvent() {
        Logger logger = Logger.getLogger("com.example");
        logger.setLevel(Level.parse("DEBUG"));
        assertEquals("DEBUG", logger.getLevel().getName());
        Map<String, String> config = new HashMap<>();
        config.put(String.format(quarkusCategoryLevelTemplate, "org.qubership.cloud"), "TRACE");
        config.put(String.format(quarkusCategoryLevelTemplate, "com.example"), "TRACE");

        loggerUpdater.onConfigUpdated(new ConfigUpdatedEvent(config,""));

        verify(loggerUpdater).updateLogLevel(String.format(quarkusCategoryLevelTemplate, "org.qubership.cloud"), "TRACE");
        verify(loggerUpdater).updateLogLevel(String.format(quarkusCategoryLevelTemplate, "com.example"), "TRACE");

        config.remove(String.format(quarkusCategoryLevelTemplate, "com.example"));
        loggerUpdater.onConfigUpdated(new ConfigUpdatedEvent(config, ""));
        verify(loggerUpdater, times(2)).updateLogLevel(String.format(quarkusCategoryLevelTemplate, "org.qubership.cloud"), "TRACE");
        verify(loggerUpdater).updateLogLevel(String.format(quarkusCategoryLevelTemplate, "com.example"), "DEBUG"); // return to the previous level

        Map<String, String> logLevelSnapshot = loggerUpdater.getLogLevelSnapshot();
        assertEquals(1, logLevelSnapshot.size());
    }
}

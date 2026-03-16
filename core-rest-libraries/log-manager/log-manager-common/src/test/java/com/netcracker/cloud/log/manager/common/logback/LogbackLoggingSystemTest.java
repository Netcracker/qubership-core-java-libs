package com.netcracker.cloud.log.manager.common.logback;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.joran.spi.JoranException;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LogbackLoggingSystemTest {

    @Test
    void testGetLogLevels() throws JoranException {
        registerLog("com.netcracker.cloud", Level.TRACE);
        registerLog("com.netcracker.cloud.dbaas", Level.DEBUG);

        LogbackLoggingSystem loggingSystem = new LogbackLoggingSystem();

        Map<String, String> logLevels = loggingSystem.getLogLevels();
        assertEquals("TRACE", logLevels.get("com.netcracker.cloud"));
        assertEquals("DEBUG", logLevels.get("com.netcracker.cloud.dbaas"));
    }

    private static void registerLog(String logName, Level level) {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        Logger logger = loggerContext.getLogger(logName);
        logger.setLevel(level);
    }
}

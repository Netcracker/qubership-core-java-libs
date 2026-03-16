package com.netcracker.cloud.log.manager.common.log4j2;


import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.spi.ExtendedLogger;
import org.apache.logging.log4j.spi.LoggerContext;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class Log4j2LoggingSystemTest {

    @Test
    void test() {
        registerLog("com.netcracker.cloud", Level.DEBUG);
        registerLog("com.netcracker.cloud.dbaas", Level.TRACE);
        Log4j2LoggingSystem log4j2LoggingSystem = new Log4j2LoggingSystem();

        Map<String, String> logLevels = log4j2LoggingSystem.getLogLevels();

        assertEquals("DEBUG", logLevels.get("com.netcracker.cloud"));
        assertEquals("TRACE", logLevels.get("com.netcracker.cloud.dbaas"));
    }

    private void registerLog(String logName, Level level) {
        LoggerContext loggerContext = LogManager.getContext(false);
        ExtendedLogger logger = loggerContext.getLogger(logName);
        Configurator.setLevel(logger.getName(), level);
    }
}

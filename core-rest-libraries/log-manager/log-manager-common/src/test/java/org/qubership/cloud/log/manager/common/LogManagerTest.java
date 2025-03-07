package org.qubership.cloud.log.manager.common;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import org.apache.logging.log4j.Logger;
import org.jboss.logmanager.LogContext;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LogManagerTest {

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger();

    @Test
    void testGetLogLevel() {
        LogContext context = LogContext.getLogContext();
        org.jboss.logmanager.Logger testLogger1 = context.getLogger("TestLoggerJboss");
        testLogger1.setLevel(org.jboss.logmanager.Level.INFO);
        registerLogbackLog("com.example.TestLoggerLogback", Level.WARN);


        Map<String, String> logLevels = LogManager.getLogLevel();
        assertEquals("INFO", logLevels.get("TestLoggerJboss"), "Expected JBossLogger to have INFO level.");
        assertEquals("WARN", logLevels.get("com.example.TestLoggerLogback"), "Expected LogbackLogger to have DEBUG level.");
    }

    private static void registerLogbackLog(String logName, Level level) {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        ch.qos.logback.classic.Logger logger = loggerContext.getLogger(logName);
        logger.setLevel(level);
    }
}

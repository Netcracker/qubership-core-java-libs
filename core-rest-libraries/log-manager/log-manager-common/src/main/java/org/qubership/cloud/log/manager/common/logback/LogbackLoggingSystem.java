package org.qubership.cloud.log.manager.common.logback;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import org.qubership.cloud.log.manager.common.LoggingSystem;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class LogbackLoggingSystem implements LoggingSystem {
    @Override
    public Map<String, String> getLogLevels() {
        LoggerContext loggerFactory = getLoggerFactory();
        Map<String, String> logLevels = new HashMap<>();
        for (Logger logger : loggerFactory.getLoggerList()) {
            logLevels.put(logger.getName(), logger.getEffectiveLevel().toString());
        }
        return logLevels;
    }

    private LoggerContext getLoggerFactory() {
        return (LoggerContext) LoggerFactory.getILoggerFactory();
    }
}

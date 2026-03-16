package com.netcracker.cloud.log.manager.common.jboss;

import com.netcracker.cloud.log.manager.common.LoggingSystem;
import lombok.extern.slf4j.Slf4j;
import org.jboss.logmanager.LogContext;
import org.jboss.logmanager.Logger;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class JBossLoggingSystem implements LoggingSystem {

    @Override
    public Map<String, String> getLogLevels() {
        Map<String, String> loggerLevels = new HashMap<>();
        LogContext logContext = LogContext.getLogContext();

        Enumeration<String> loggerNames = logContext.getLoggerNames();
        while (loggerNames.hasMoreElements()) {
            String loggerName = loggerNames.nextElement();
            Logger logger = logContext.getLogger(loggerName);
            if (logger != null) {
                loggerLevels.put(loggerName, getEffectiveLogLevel(logger));
            }
        }
        addRootLevel(loggerLevels, logContext);
        return loggerLevels;
    }

    private void addRootLevel(Map<String, String> loggerLevels, LogContext logContext) {
        String rootLogName = "";
        Logger rootLogger = logContext.getLogger(rootLogName);
        if (rootLogger != null && rootLogger.getLevel() != null) {
            loggerLevels.put("ROOT", rootLogger.getLevel().getName());
        }
    }

    private String getEffectiveLogLevel(Logger logger) {
        if (logger == null) {
            return null;
        }
        if (logger.getLevel() != null) {
            return logger.getLevel().getName();
        }
        return getEffectiveLogLevel(logger.getParent());
    }
}

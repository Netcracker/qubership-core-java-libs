package org.qubership.cloud.log.manager.spring;

import org.qubership.cloud.log.manager.common.LogManager;
import org.slf4j.LoggerFactory;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class LoggingUpdater implements ApplicationListener<EnvironmentChangeEvent>, Ordered {


    private static final org.slf4j.Logger log = LoggerFactory.getLogger(LoggingUpdater.class);
    private static final String LOGGING_LEVEL_PREFIX = "logging.level.";

    private final Environment environment;
    private final Map<String, String> logLevelSnapshot = new HashMap<>();

    public LoggingUpdater(Environment environment) {
        this.environment = environment;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    public void onApplicationEvent(EnvironmentChangeEvent event) {
        Set<String> changedKeys = event.getKeys().stream()
                .filter(key -> key.startsWith(LOGGING_LEVEL_PREFIX))
                .collect(Collectors.toSet());

        if (changedKeys.isEmpty()) {
            return;
        }

        StringJoiner unsuccessfullyUpdatedCategories = new StringJoiner(",");

        for (String key : changedKeys) {
            String loggerName = key.substring(LOGGING_LEVEL_PREFIX.length());
            String newLevel = environment.getProperty(key);
            if (newLevel == null) {
                String defaultLevel = logLevelSnapshot.get(loggerName) == null ? getEffectiveParentLogLevel(loggerName) : logLevelSnapshot.get(loggerName);
                if (!updateLogLevel(loggerName, defaultLevel)) {
                    unsuccessfullyUpdatedCategories.add(loggerName);
                }
                logLevelSnapshot.remove(loggerName);
            }
            else {
                saveCurrentLogLevel(loggerName);
                //Do nothing, LoggingRebinder will update this logger
            }
        }
        if (unsuccessfullyUpdatedCategories.length() > 0) {
            throw new RuntimeException("Cannot update log levels for the following properties: " + unsuccessfullyUpdatedCategories);
        }
    }

    private void saveCurrentLogLevel(String loggerName) {
        if (!logLevelSnapshot.containsKey(loggerName)) {
            String currentLevel = getLogLevel(loggerName);
            logLevelSnapshot.put(loggerName, currentLevel);
        }
    }

    private boolean updateLogLevel(String loggerName, String level) {
        try {
            if (loggerName.equalsIgnoreCase("root")) {
                loggerName = null;
            }
            LoggingSystem system = LoggingSystem.get(LoggingSystem.class.getClassLoader());
            system.setLogLevel(loggerName, resolveLogLevel(level));
            return true;
        } catch (RuntimeException ex) {
            log.error("Cannot set level: {} for '{}'", level, loggerName);
            return false;
        }
    }

    private String getLogLevel(String loggerName) {
        String logLevel;
        if (loggerName.equalsIgnoreCase("root")){
            logLevel = LogManager.getLogLevel().get("ROOT");
        }
        else{
            logLevel = LogManager.getLogLevel().get(loggerName);
        }
        return logLevel;
    }

    private String getEffectiveParentLogLevel(String loggerName) {
        if (loggerName.equalsIgnoreCase("root")) return "INFO";
        Logger logger = Logger.getLogger(loggerName);
        Logger currentLogger = logger.getParent();

        while (currentLogger != null) {
            Level level = currentLogger.getLevel();
            if (level != null) {
                return level.getName();
            }
            currentLogger = currentLogger.getParent();
        }

        return Logger.getLogger("").getLevel().getName();
    }

    private LogLevel resolveLogLevel(String level) {
        String trimmedLevel = level.trim();
        if ("false".equalsIgnoreCase(trimmedLevel)) {
            return LogLevel.OFF;
        }
        return LogLevel.valueOf(trimmedLevel.toUpperCase(Locale.ENGLISH));
    }
}

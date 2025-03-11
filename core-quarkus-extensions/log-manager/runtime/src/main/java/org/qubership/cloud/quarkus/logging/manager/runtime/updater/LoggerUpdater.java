package org.qubership.cloud.quarkus.logging.manager.runtime.updater;

import org.qubership.cloud.quarkus.logging.manager.runtime.updater.event.ConfigUpdatedEvent;
import org.qubership.cloud.quarkus.logging.manager.runtime.updater.event.LogUpdateEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@ApplicationScoped
public class LoggerUpdater {

    private static final org.jboss.logging.Logger thisClassLogger = org.jboss.logging.Logger.getLogger(LoggerUpdater.class);
    private static final String QUARKUS_LOG_LVL_CONFIG_PREFIX = "quarkus.log.category.";
    private static final String QUARKUS_LOG_LVL_CONFIG_SUFFIX = ".level";
    private static final String QUARKUS_ROOT_LOG_LVL_PROPERTY = "quarkus.log.level";
    private static final String LOGGING_LEVEL_PROPERTY_PREFIX = "logging.level.";

    private final Map<String, String> logLevelSnapshot = new HashMap<>();

    void onConfigUpdated(@Observes ConfigUpdatedEvent task) {
        thisClassLogger.debug("Config updated, search for logger properties to change logger levels");
        StringJoiner unsuccessfullyUpdatedCategories = new StringJoiner(",");
        unsuccessfullyUpdatedCategories.setEmptyValue("");

        Map<String, String> logNamesFromConfig = task.getProperties()
                .entrySet()
                .stream()
                .filter(entry -> isLogProperty(entry.getKey()))
                .filter(entry -> !thisClassLogger.getName().equals(extractLoggerNameFromProperty(entry.getKey())))
                .filter(entry -> entry.getValue() != null)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue
                ));


        Map<String, String> logsToUpdate = new HashMap<>(logLevelSnapshot);
        logNamesFromConfig
                .entrySet()
                .stream()
                .filter(entry -> !entry.getValue().isBlank())
                .forEach(entry -> logsToUpdate.put(entry.getKey(), entry.getValue()));


        logsToUpdate.forEach((logName, logLevel) -> {
            if (!updateLogLevel(logName, logLevel)) {
                unsuccessfullyUpdatedCategories.add(logName);
            }
        });
        removeDeletedLogsFromSnapshot(logNamesFromConfig.keySet().stream().toList());

        if (unsuccessfullyUpdatedCategories.length() > 0) {
            throw new RuntimeException("Cannot update log levels for the following properties: " + unsuccessfullyUpdatedCategories);
        }
    }

    void removeDeletedLogsFromSnapshot(List<String> logsFromConsul) {
        logLevelSnapshot.keySet().retainAll(logsFromConsul);
    }

    void onLogUpdate(@Observes LogUpdateEvent event) {
        Map<String, String> properties = event.getProperties();
        if (properties == null || properties.isEmpty()) {
            return;
        }
        Map<String, String> logLevels = properties
                .entrySet()
                .stream()
                .filter(entry -> entry.getKey().startsWith(LOGGING_LEVEL_PROPERTY_PREFIX))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue
                ));

        logLevels.forEach(this::updateLogLevel);
    }

    // 1. is root quarkus property quarkus.log.level
    // or
    // starts from quarkus.log.category. and ends to .level
    // or
    // starts from logging.level.
    private boolean isLogProperty(String propName) {
        return propName.equals(QUARKUS_ROOT_LOG_LVL_PROPERTY) ||
                (propName.startsWith(QUARKUS_LOG_LVL_CONFIG_PREFIX) && propName.endsWith(QUARKUS_LOG_LVL_CONFIG_SUFFIX)) ||
                (propName.startsWith(LOGGING_LEVEL_PROPERTY_PREFIX));
    }

    boolean updateLogLevel(String propertyName, String propValue) {
        Level levelValue = parseLevelSafe(propValue);
        if (levelValue == null) {
            return false;
        }
        putToSnapshot(propertyName);
        String loggerName = extractLoggerNameFromProperty(propertyName);
        if (isRootLogger(loggerName)) {
            getLogger("").setLevel(levelValue);
        } else {
            getLogger(loggerName).setLevel(levelValue);
        }
        thisClassLogger.debugv("Successfully changed logger level to {0} for {1}", levelValue.getName(), propertyName);
        return true;
    }

    private boolean isRootLogger(String loggerName) {
        return loggerName.isEmpty() || loggerName.equalsIgnoreCase("root");
    }

    private void putToSnapshot(String propertyName) {
        logLevelSnapshot.computeIfAbsent(propertyName, key -> {
            Logger logger = getLogger(extractLoggerNameFromProperty(key));
            return getLogLevel(logger);
        });
    }

    private String getLogLevel(Logger logger) {
        if (logger == null) {
            return null;
        }
        if (logger.getLevel() != null) {
            return logger.getLevel().getName();
        }
        return getLogLevel(logger.getParent());
    }


    private String extractLoggerNameFromProperty(String logName) {
        logName = logName.replaceFirst(QUARKUS_LOG_LVL_CONFIG_PREFIX, "")
                .replaceFirst(LOGGING_LEVEL_PROPERTY_PREFIX, "")
                .replaceFirst(QUARKUS_ROOT_LOG_LVL_PROPERTY, "");
        if (logName.endsWith(QUARKUS_LOG_LVL_CONFIG_SUFFIX)) {
            logName = logName.substring(0, logName.length() - QUARKUS_LOG_LVL_CONFIG_SUFFIX.length());
        }
        return sanitizeLoggerCategory(logName);
    }

    // for testing purpose
    Map<String, String> getLogLevelSnapshot() {
        return new HashMap<>(logLevelSnapshot);
    }

    private Logger getLogger(String category) {
        return Logger.getLogger(category);
    }
    private String sanitizeLoggerCategory(String category) {
        return category.replaceAll("^\"|\"$", "");
    }


    private Level parseLevelSafe(String logLevel) {
        try {
            return Level.parse(logLevel.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            thisClassLogger.warnv("Cannot parse logger level: {0}", e.getMessage());
            return null;
        }
    }
}
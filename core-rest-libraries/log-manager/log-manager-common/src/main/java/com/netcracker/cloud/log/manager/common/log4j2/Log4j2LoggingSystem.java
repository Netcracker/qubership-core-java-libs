package com.netcracker.cloud.log.manager.common.log4j2;

import com.netcracker.cloud.log.manager.common.LoggingSystem;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.util.NameUtil;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
public class Log4j2LoggingSystem implements LoggingSystem {
    @Override
    public Map<String, String> getLogLevels() {
        Map<String, String> loggerLevels = new HashMap<>();
        LoggerContext context = (LoggerContext) LogManager.getContext(false);

        Map<String, LoggerConfig> loggers = new LinkedHashMap<>();
        for (Logger logger : context.getLoggers()) {
            addLogger(loggers, logger.getName(), context);
        }
        for (Map.Entry<String, LoggerConfig> log : loggers.entrySet()) {
            loggerLevels.put(log.getKey(), log.getValue().getLevel().name());
        }
        return loggerLevels;
    }

    private void addLogger(Map<String, LoggerConfig> loggers, String name, LoggerContext context) {
        Configuration configuration = context.getConfiguration();
        while (name != null) {
            if (name.isEmpty()) {
                loggers.computeIfAbsent("ROOT", configuration::getLoggerConfig);
            }
            else {
                loggers.computeIfAbsent(name, configuration::getLoggerConfig);
            }
            name = getSubName(name);
        }
    }

    private String getSubName(String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        int nested = name.lastIndexOf('$');
        return (nested != -1) ? name.substring(0, nested) : NameUtil.getSubName(name);
    }
}

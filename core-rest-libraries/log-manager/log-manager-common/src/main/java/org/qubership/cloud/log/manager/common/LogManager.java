package org.qubership.cloud.log.manager.common;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LogManager {
    private static final List<LoggingSystem> loggingSystems = LoggingSystemFactory.get();

    public static Map<String, String> getLogLevel() {
        Map<String, String> logLevels = new HashMap<>();
        for (LoggingSystem loggingSystem : loggingSystems) {
            logLevels.putAll(loggingSystem.getLogLevels());
        }
        return logLevels;
    }
}

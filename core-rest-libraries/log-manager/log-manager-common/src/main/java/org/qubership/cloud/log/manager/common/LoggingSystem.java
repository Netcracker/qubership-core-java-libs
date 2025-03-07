package org.qubership.cloud.log.manager.common;

import java.util.Map;

public interface LoggingSystem {
    Map<String, String> getLogLevels();

    default void setLogLevel(String logName, String level) {
    }
}

package org.qubership.cloud.quarkus.logging.manager.runtime;

import org.qubership.cloud.log.manager.common.LogManager;

import java.util.Map;

public class LogController {

    private LogController() {
    }

    public static Map<String, String> getLoggers() {
        return LogManager.getLogLevel();
    }
}

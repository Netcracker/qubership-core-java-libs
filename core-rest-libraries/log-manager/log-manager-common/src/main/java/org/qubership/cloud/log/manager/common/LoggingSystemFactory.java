package org.qubership.cloud.log.manager.common;

import org.qubership.cloud.log.manager.common.jboss.JBossLoggingSystemProvider;
import org.qubership.cloud.log.manager.common.log4j2.Log4j2LoggingSystemProvider;
import org.qubership.cloud.log.manager.common.logback.LogbackLoggingSystemProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

class LoggingSystemFactory {

    static List<LoggingSystemProvider> loggingSystemProviders = new ArrayList<>();

    static{
        loggingSystemProviders.add(new JBossLoggingSystemProvider());
        loggingSystemProviders.add(new Log4j2LoggingSystemProvider());
        loggingSystemProviders.add(new LogbackLoggingSystemProvider());
    }

    static List<LoggingSystem> get() {
        return loggingSystemProviders.stream()
                .map(LoggingSystemProvider::provide)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}
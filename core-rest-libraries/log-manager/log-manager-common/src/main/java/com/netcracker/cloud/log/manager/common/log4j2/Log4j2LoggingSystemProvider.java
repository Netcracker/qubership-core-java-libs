package com.netcracker.cloud.log.manager.common.log4j2;

import com.netcracker.cloud.log.manager.common.LogUtils;
import com.netcracker.cloud.log.manager.common.LoggingSystem;
import com.netcracker.cloud.log.manager.common.LoggingSystemProvider;

public class Log4j2LoggingSystemProvider implements LoggingSystemProvider {

    private final boolean PRESENT;

    public Log4j2LoggingSystemProvider() {
        PRESENT = LogUtils
                .isPresent("org.apache.logging.log4j.core.impl.Log4jContextFactory", Log4j2LoggingSystemProvider.class.getClassLoader());

    }

    // only for testing purpose
    Log4j2LoggingSystemProvider(boolean present) {
        PRESENT = present;
    }

    @Override
    public LoggingSystem provide() {
        if (PRESENT) {
            return new Log4j2LoggingSystem();
        }
        return null;
    }
}

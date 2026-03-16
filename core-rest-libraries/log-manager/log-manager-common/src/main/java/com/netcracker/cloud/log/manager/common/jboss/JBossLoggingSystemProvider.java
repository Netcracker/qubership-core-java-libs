package com.netcracker.cloud.log.manager.common.jboss;

import com.netcracker.cloud.log.manager.common.LogUtils;
import com.netcracker.cloud.log.manager.common.LoggingSystem;
import com.netcracker.cloud.log.manager.common.LoggingSystemProvider;

public class JBossLoggingSystemProvider implements LoggingSystemProvider {
    private final boolean PRESENT;

    public JBossLoggingSystemProvider() {
        PRESENT = LogUtils
                .isPresent("org.jboss.logmanager.LogContext", JBossLoggingSystemProvider.class.getClassLoader());
    }

    // only for testing purpose
    JBossLoggingSystemProvider(boolean present) {
        PRESENT = present;
    }


    @Override
    public LoggingSystem provide() {
        if (PRESENT) {
            return new JBossLoggingSystem();
        }
        return null;
    }
}

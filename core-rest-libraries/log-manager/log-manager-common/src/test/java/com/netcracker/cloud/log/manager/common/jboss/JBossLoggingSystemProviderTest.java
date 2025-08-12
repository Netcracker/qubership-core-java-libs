package com.netcracker.cloud.log.manager.common.jboss;

import com.netcracker.cloud.log.manager.common.LoggingSystem;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class JBossLoggingSystemProviderTest {


    @Test
    void testProvideWhenJBossLoggingIsPresent() {
        JBossLoggingSystemProvider provider = new JBossLoggingSystemProvider();
        LoggingSystem loggingSystem = provider.provide();
        assertNotNull(loggingSystem);
        assert(loggingSystem instanceof JBossLoggingSystem);
    }

    @Test
    void testProvideWhenJBossLoggingNotPresent() throws Exception {
        JBossLoggingSystemProvider provider = new JBossLoggingSystemProvider(false);
        LoggingSystem loggingSystem = provider.provide();
        assertNull(loggingSystem);
    }
}

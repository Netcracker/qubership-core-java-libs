package org.qubership.cloud.log.manager.common.jboss;

import org.jboss.logmanager.Level;
import org.jboss.logmanager.LogContext;
import org.jboss.logmanager.Logger;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JBossLoggingSystemTest {

    @Test
    void testGetLogLevels() {
        JBossLoggingSystem loggingSystem = new JBossLoggingSystem();
        LogContext context = LogContext.getLogContext();
        Logger testLogger1 = context.getLogger("TestLogger1");
        testLogger1.setLevel(org.jboss.logmanager.Level.INFO);

        Logger testLogger2 = context.getLogger("TestLogger2");
        testLogger2.setLevel(org.jboss.logmanager.Level.DEBUG);

        Map<String, String> logLevels = loggingSystem.getLogLevels();

        assertEquals("INFO", logLevels.get("TestLogger1"));
        assertEquals("DEBUG", logLevels.get("TestLogger2"));
    }

    @Test
    void testGetLogLevels_rootLogLevel() {
        JBossLoggingSystem loggingSystem = new JBossLoggingSystem();
        LogContext context = LogContext.getLogContext();
        Logger testLogger1 = context.getLogger("ROOT");
        testLogger1.setLevel(org.jboss.logmanager.Level.INFO);

        Map<String, String> logLevels = loggingSystem.getLogLevels();

        assertEquals("INFO", logLevels.get("ROOT"));
    }


    @Test
    void testGetLogLevels_getLogLevelFromParent() {
        JBossLoggingSystem loggingSystem = new JBossLoggingSystem();
        LogContext context = LogContext.getLogContext();
        Logger testLogger1 = context.getLogger("org.qubership");
        testLogger1.setLevel(Level.DEBUG);
        Logger dbaasLog = context.getLogger("org.qubership.dbaas");

        Map<String, String> logLevels = loggingSystem.getLogLevels();

        assertEquals("DEBUG", logLevels.get("org.qubership.dbaas"));
    }
}

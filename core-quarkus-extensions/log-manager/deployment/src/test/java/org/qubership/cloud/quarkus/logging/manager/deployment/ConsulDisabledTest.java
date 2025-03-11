package org.qubership.cloud.quarkus.logging.manager.deployment;

import org.qubership.cloud.log.manager.common.LogManager;
import io.quarkus.arc.Arc;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.enterprise.event.Event;
import org.junit.jupiter.api.Test;

import static java.lang.Thread.sleep;
import static org.junit.jupiter.api.Assertions.assertNull;

@QuarkusTest
class ConsulDisabledTest {
    @Test
    void testPropertyNotLoadedFromConsul() throws InterruptedException {
        Event<Object> event = Arc.container().beanManager().getEvent();
        event.fire(new StartupEvent());
        sleep(5000);
        String level = LogManager.getLogLevel().get("com.example.consul.disabled");
        assertNull(level);
    }
}
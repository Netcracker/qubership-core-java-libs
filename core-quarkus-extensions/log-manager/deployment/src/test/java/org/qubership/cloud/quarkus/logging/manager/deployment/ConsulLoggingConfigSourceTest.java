package org.qubership.cloud.quarkus.logging.manager.deployment;

import org.qubership.cloud.log.manager.common.LogManager;
import io.quarkus.arc.Arc;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.enterprise.event.Event;
import org.junit.jupiter.api.Test;

import java.util.logging.Level;

import static java.lang.Thread.sleep;
import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
@QuarkusTestResource(ConsulContainerResource.class)
class ConsulLoggingConfigSourceTest {
    @Test
    void testPropertyLoadedFromConsul() throws InterruptedException {
        Event<Object> event = Arc.container().beanManager().getEvent();
        event.fire(new StartupEvent());
        sleep(5000);
        String level = LogManager.getLogLevel().get("com.example.cloud");
        assertEquals(Level.FINE.toString(), level);
    }

}

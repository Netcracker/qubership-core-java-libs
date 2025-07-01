package org.qubership.cloud.quarkus.logging.manager.deployment;

import org.qubership.cloud.log.manager.common.LogManager;
import io.quarkus.arc.Arc;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.enterprise.event.Event;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNull;

@QuarkusTest
@TestProfile(ConsulDisabledTest.Profile.class)
class ConsulDisabledTest {
    @Test
    void testPropertyNotLoadedFromConsul() {
        Event<Object> event = Arc.container().beanManager().getEvent();
        event.fire(new StartupEvent());

        String level = LogManager.getLogLevel().get("com.example.consul.disabled");
        assertNull(level);
    }

    public static class Profile implements QuarkusTestProfile {
        public Profile() {
        }

        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("cloud.microservice.namespace", "test-ns",
                    "cloud.microservice.name", "test-ms");
        }
    }
}

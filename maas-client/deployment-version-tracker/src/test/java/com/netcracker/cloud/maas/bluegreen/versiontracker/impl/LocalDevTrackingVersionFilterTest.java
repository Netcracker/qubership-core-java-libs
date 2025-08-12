package com.netcracker.cloud.maas.bluegreen.versiontracker.impl;

import com.netcracker.cloud.bluegreen.api.model.BlueGreenState;
import com.netcracker.cloud.bluegreen.api.model.NamespaceVersion;
import com.netcracker.cloud.bluegreen.api.model.Version;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static com.netcracker.cloud.bluegreen.api.model.State.ACTIVE;
import static com.netcracker.cloud.bluegreen.api.model.State.CANDIDATE;
import static com.netcracker.cloud.bluegreen.impl.service.ConsulBlueGreenStatePublisher.UNKNOWN_DATETIME;
import static com.netcracker.cloud.maas.client.impl.Env.PROP_NAMESPACE;
import static org.junit.jupiter.api.Assertions.*;

class LocalDevTrackingVersionFilterTest {

    @Test
    void testDefaultConstructor() {
        // by default filter is set to `true` and ignores any version
        withProp(PROP_NAMESPACE, "core-dev", () -> {
            var filter = new LocalDevTrackingVersionFilter();
            assertTrue(filter.test("v1"));
            assertTrue(filter.test(""));
            assertTrue(filter.test("v2"));
        });
    }

    @Test
    void testSetFilterState() {
        withProp(PROP_NAMESPACE, "core-dev", () -> {
            var state = new BlueGreenState(
                    new NamespaceVersion("core-dev-1", ACTIVE, new Version("v1")),
                    new NamespaceVersion("core-dev-2", CANDIDATE, new Version("v2")),
                    OffsetDateTime.now());

            var filter = new LocalDevTrackingVersionFilter(state);

            assertTrue(filter.test("v1"));
            assertTrue(filter.test(""));
            assertFalse(filter.test("v2"));
            assertEquals(state, filter.getState());
        });
    }

    @Test
    void testNewInstanceWithoutPropNamespace() {
        var filter = new LocalDevTrackingVersionFilter("test-namespace", new Version("v1"));
        BlueGreenState expectedState = new BlueGreenState(new NamespaceVersion("test-namespace", ACTIVE, new Version("v1")), UNKNOWN_DATETIME);
        assertEquals(expectedState, filter.getState());
    }

    @FunctionalInterface
    interface OmnivoreRunnable {
        void run() throws Exception;
    }

    private static void withProp(String prop, String value, OmnivoreRunnable test) {
        String save = System.getProperty(prop);
        if (value == null) {
            System.clearProperty(prop);
        } else {
            System.setProperty(prop, value);
        }

        try {
            test.run();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (save == null) {
                System.clearProperty(prop);
            } else {
                System.setProperty(prop, save);
            }
        }
    }
}

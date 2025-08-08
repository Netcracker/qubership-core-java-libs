package com.netcracker.cloud.maas.client.quarkus;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import org.junit.jupiter.api.Test;
import org.qubership.cloud.bluegreen.api.service.BlueGreenStatePublisher;
import org.qubership.cloud.bluegreen.impl.service.InMemoryBlueGreenStatePublisher;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class MaasLocalDevConfigTest {

    @Inject
    Instance<BlueGreenStatePublisher> blueGreenStatePublisher;

    @Test
    void testBlueGreenStatePublisherNotProducedWhenPropertyNotSet() {
        assertFalse(blueGreenStatePublisher.isResolvable());
    }
}

@QuarkusTest
@TestProfile(MaasLocalDevConfigEnabledTest.EnabledProfile.class)
class MaasLocalDevConfigEnabledTest extends BaseMaasLocalDevConfigTest {

    @Test
    void testBlueGreenStatePublisherProducedWhenPropertyEnabled() {
        assertTrue(blueGreenStatePublisher.isResolvable());
        BlueGreenStatePublisher publisher = blueGreenStatePublisher.get();
        assertNotNull(publisher);
        assertInstanceOf(InMemoryBlueGreenStatePublisher.class, publisher);
    }

    public static class EnabledProfile extends BaseTestProfile {
        @Override
        public String getConfigProfile() {
            return "test-enabled";
        }

        @Override
        public Map<String, String> getConfigOverrides() {
            return createConfigOverrides(true);
        }
    }
}

@QuarkusTest
@TestProfile(MaasLocalDevConfigDisabledTest.DisabledProfile.class)
class MaasLocalDevConfigDisabledTest extends BaseMaasLocalDevConfigTest {

    @Test
    void testBlueGreenStatePublisherNotProducedWhenPropertyDisabled() {
        assertFalse(blueGreenStatePublisher.isResolvable());
    }

    public static class DisabledProfile extends BaseTestProfile {
        @Override
        public String getConfigProfile() {
            return "test-disabled";
        }

        @Override
        public Map<String, String> getConfigOverrides() {
            return createConfigOverrides(false);
        }
    }
}

abstract class BaseMaasLocalDevConfigTest {
    @Inject
    Instance<BlueGreenStatePublisher> blueGreenStatePublisher;
}

abstract class BaseTestProfile implements QuarkusTestProfile {
    private static final String NAMESPACE = "test-namespace";

    protected Map<String, String> createConfigOverrides(boolean enabled) {
        return Map.of(
            "maas.local-dev.enabled", String.valueOf(enabled),
            "cloud.microservice.namespace", NAMESPACE
        );
    }
}

package org.qubership.cloud.maas.client.quarkus;

import org.qubership.cloud.bluegreen.api.model.BlueGreenState;
import org.qubership.cloud.bluegreen.api.service.BlueGreenStatePublisher;
import org.qubership.cloud.maas.client.bluegreen.rabbit.DynamicQueueBindingsManager;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.function.Consumer;

import static io.smallrye.common.constraint.Assert.assertNotNull;

@QuarkusTest
class MaaSClientConfigTest {
    @Inject
    DynamicQueueBindingsManager dynamicQueueBindingsManager;

    @Test
    void testMaaSClient() {
        assertNotNull(dynamicQueueBindingsManager);
    }

    @ApplicationScoped
    static class TestBlueGreenStatePublisher implements BlueGreenStatePublisher {
        @Override
        public void subscribe(Consumer<BlueGreenState> consumer) {
        }

        @Override
        public void unsubscribe(Consumer<BlueGreenState> consumer) {
        }

        @Override
        public BlueGreenState getBlueGreenState() {
            return null;
        }
    }
}
package org.qubership.cloud.bluegreen.quarkus.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.qubership.cloud.bluegreen.api.model.BlueGreenState;
import org.qubership.cloud.bluegreen.api.model.State;
import org.qubership.cloud.bluegreen.api.model.Version;
import org.qubership.cloud.bluegreen.api.service.BlueGreenStatePublisher;
import org.qubership.cloud.bluegreen.api.service.GlobalMutexService;
import org.qubership.cloud.bluegreen.api.service.MicroserviceMutexService;
import org.qubership.cloud.bluegreen.impl.service.InMemoryBlueGreenStatePublisher;
import org.qubership.cloud.bluegreen.impl.service.InMemoryGlobalMutexService;
import org.qubership.cloud.bluegreen.impl.service.InMemoryMicroserviceMutexService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class InMemoryConfigurationTest {

    private InMemoryConfiguration configuration;
    private static final String TEST_NAMESPACE = "test-namespace";

    @BeforeEach
    void setUp() {
        configuration = new InMemoryConfiguration();
        configuration.namespace = TEST_NAMESPACE;
    }

    @Test
    void shouldCreateInMemoryBlueGreenStatePublisherWithCorrectState() {
        BlueGreenStatePublisher publisher = configuration.inMemoryBlueGreenStatePublisher();

        assertNotNull(publisher);
        assertInstanceOf(InMemoryBlueGreenStatePublisher.class, publisher);

        BlueGreenState state = publisher.getBlueGreenState();
        assertNotNull(state);
        assertEquals(TEST_NAMESPACE, state.getCurrent().getNamespace());
        assertEquals(State.ACTIVE, state.getCurrent().getState());
        assertEquals(new Version("v1"), state.getCurrent().getVersion());
    }

    @Test
    void shouldCreateInMemoryGlobalMutexServiceInstance() {
        GlobalMutexService service = configuration.inMemoryGlobalMutexService();

        assertNotNull(service);
        assertInstanceOf(InMemoryGlobalMutexService.class, service);
    }

    @Test
    void shouldCreateInMemoryMicroserviceMutexServiceInstance() {
        MicroserviceMutexService service = configuration.imMemoryMicroserviceMutexService();

        assertNotNull(service);
        assertInstanceOf(InMemoryMicroserviceMutexService.class, service);
    }
}

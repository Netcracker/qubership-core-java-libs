package org.qubership.cloud.bluegreen.quarkus.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.qubership.cloud.bluegreen.api.service.MicroserviceMutexService;
import org.qubership.cloud.bluegreen.impl.service.ConsulMicroserviceMutexService;
import org.qubership.cloud.consul.provider.common.TokenStorage;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

class ConsulBlueGreenMicroserviceMutexConfigurationTest {

    private ConsulBlueGreenMicroserviceMutexConfiguration configuration;
    private static final String TEST_TOKEN = "test-token";
    private static final String CONSUL_URL = "http://localhost:8500";
    private static final String NAMESPACE = "test-namespace";
    private static final String SERVICE_NAME = "test-service";
    private static final String POD_NAME = "test-pod";

    @BeforeEach
    void setUp() {
        configuration = new ConsulBlueGreenMicroserviceMutexConfiguration();
        configuration.consulUrl = CONSUL_URL;
        configuration.namespace = NAMESPACE;
        configuration.name = SERVICE_NAME;
        configuration.pod = Optional.of(POD_NAME);
    }

    @Test
    void testMicroserviceMutexServiceCreation() {
        TokenStorage tokenStorage = mock(TokenStorage.class);
        when(tokenStorage.get()).thenReturn(TEST_TOKEN);

        MicroserviceMutexService service = configuration.microserviceMutexService(tokenStorage);

        assertNotNull(service);
        assertInstanceOf(ConsulMicroserviceMutexService.class, service);
    }

    @Test
    void testMicroserviceMutexServiceWithDefaultPodName() {
        TokenStorage tokenStorage = mock(TokenStorage.class);
        when(tokenStorage.get()).thenReturn(TEST_TOKEN);
        configuration.pod = Optional.empty();

        MicroserviceMutexService service = configuration.microserviceMutexService(tokenStorage);

        assertNotNull(service);
        assertInstanceOf(ConsulMicroserviceMutexService.class, service);
    }

    @Test
    void testCloseWithAutoCloseableService() throws Exception {
        MicroserviceMutexService service = mock(MicroserviceMutexService.class, 
            withSettings().extraInterfaces(AutoCloseable.class));

        configuration.close(service);

        verify((AutoCloseable) service).close();
    }
}

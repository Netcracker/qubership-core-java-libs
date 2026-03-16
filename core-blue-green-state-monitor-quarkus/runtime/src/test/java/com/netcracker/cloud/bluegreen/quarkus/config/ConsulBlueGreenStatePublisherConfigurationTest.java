package com.netcracker.cloud.bluegreen.quarkus.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.netcracker.cloud.bluegreen.api.service.BlueGreenStatePublisher;
import com.netcracker.cloud.bluegreen.impl.service.ConsulBlueGreenStatePublisher;
import com.netcracker.cloud.consul.provider.common.TokenStorage;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

class ConsulBlueGreenStatePublisherConfigurationTest {

    private ConsulBlueGreenStatePublisherConfiguration configuration;
    private static final String TEST_TOKEN = "test-token";
    private static final String CONSUL_URL = "http://localhost:8500";
    private static final String NAMESPACE = "test-namespace";

    @BeforeEach
    void setUp() {
        configuration = new ConsulBlueGreenStatePublisherConfiguration() {
            @Override
            public BlueGreenStatePublisher blueGreenStatePublisher(TokenStorage tokenStorage) {
                ConsulBlueGreenStatePublisher mockPublisher = mock(ConsulBlueGreenStatePublisher.class);
                when(mockPublisher.getBlueGreenState()).thenReturn(null);
                return mockPublisher;
            }
        };
        configuration.consulUrl = CONSUL_URL;
        configuration.namespace = NAMESPACE;
    }

    @Test
    void shouldCreatePublisherWithCorrectType() {
        TokenStorage tokenStorage = mock(TokenStorage.class);
        when(tokenStorage.get()).thenReturn(TEST_TOKEN);

        BlueGreenStatePublisher publisher = configuration.blueGreenStatePublisher(tokenStorage);

        assertNotNull(publisher);
        assertInstanceOf(ConsulBlueGreenStatePublisher.class, publisher);
    }

    @Test
    void shouldClosePublisherWhenItImplementsAutoCloseable() throws Exception {
        BlueGreenStatePublisher publisher = mock(BlueGreenStatePublisher.class,
            withSettings().extraInterfaces(AutoCloseable.class));

        configuration.close(publisher);

        verify((AutoCloseable) publisher).close();
    }
}

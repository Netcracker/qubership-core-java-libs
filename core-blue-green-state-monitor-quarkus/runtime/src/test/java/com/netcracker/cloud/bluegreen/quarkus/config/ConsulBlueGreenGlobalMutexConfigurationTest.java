package com.netcracker.cloud.bluegreen.quarkus.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.netcracker.cloud.bluegreen.api.service.GlobalMutexService;
import com.netcracker.cloud.bluegreen.impl.service.ConsulGlobalMutexService;
import com.netcracker.cloud.consul.provider.common.TokenStorage;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConsulBlueGreenGlobalMutexConfigurationTest {

    private ConsulBlueGreenGlobalMutexConfiguration configuration;
    private static final String TEST_TOKEN = "test-token";
    private static final Duration LOCK_TIMEOUT = Duration.ofSeconds(30);
    private static final List<String> TEST_LOCKS = List.of("test-lock");

    @BeforeEach
    void setUp() {
        configuration = new ConsulBlueGreenGlobalMutexConfiguration();
        configuration.consulUrl = "http://localhost:8500";
    }

    @Test
    void testGlobalMutexServiceCreation() {
        TokenStorage tokenStorage = mock(TokenStorage.class);
        when(tokenStorage.get()).thenReturn(TEST_TOKEN);

        GlobalMutexService service = configuration.globalMutexService(tokenStorage);

        assertNotNull(service);
        assertInstanceOf(ConsulGlobalMutexService.class, service);
    }

    @Test
    void testGlobalMutexServiceUsesTokenStorage() {
        TokenStorage tokenStorage = mock(TokenStorage.class);
        when(tokenStorage.get()).thenReturn(TEST_TOKEN);

        GlobalMutexService service = configuration.globalMutexService(tokenStorage);
        assertNotNull(service);

        try {
            service.tryLock(LOCK_TIMEOUT, TEST_LOCKS);
        } catch (Exception e) {
            // Expected exception since we're not actually connecting to Consul
        }

        verify(tokenStorage, atLeastOnce()).get();
    }
}

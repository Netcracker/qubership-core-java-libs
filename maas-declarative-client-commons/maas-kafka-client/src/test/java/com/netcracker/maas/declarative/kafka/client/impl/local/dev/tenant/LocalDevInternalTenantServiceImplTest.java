package com.netcracker.maas.declarative.kafka.client.impl.local.dev.tenant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LocalDevInternalTenantServiceImplTest {

    private static final List<String> SAMPLE_TENANTS = Arrays.asList("tenant1", "tenant2");
    private static final List<String> TENANTS_WITH_WHITESPACE = Arrays.asList(" tenant1 ", "  tenant2  ", "tenant3");
    private static final List<String> EXPECTED_TRIMMED_TENANTS = Arrays.asList("tenant1", "tenant2", "tenant3");

    private LocalDevInternalTenantServiceImpl service;

    @Mock
    private Consumer<List<String>> mockCallback;

    @BeforeEach
    void setUp() {
        service = new LocalDevInternalTenantServiceImpl(Collections::emptyList);
    }

    @Test
    void shouldTrimTenantIds() {
        // Given
        service = new LocalDevInternalTenantServiceImpl(() -> TENANTS_WITH_WHITESPACE);

        // When
        List<String> result = service.listAvailableTenants();

        // Then
        assertEquals(EXPECTED_TRIMMED_TENANTS, result);
    }

    @Test
    void shouldReturnEmptyListWhenNoTenants() {
        // When
        List<String> result = service.listAvailableTenants();

        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldReturnAllTenants() {
        // Given
        service = new LocalDevInternalTenantServiceImpl(() -> SAMPLE_TENANTS);

        // When
        List<String> result = service.listAvailableTenants();

        // Then
        assertEquals(SAMPLE_TENANTS, result);
    }

    @Test
    void shouldNotCallCallbackOnSubscribe() {
        // When
        service.subscribe(mockCallback);

        // Then
        verify(mockCallback, never()).accept(any());
    }
}

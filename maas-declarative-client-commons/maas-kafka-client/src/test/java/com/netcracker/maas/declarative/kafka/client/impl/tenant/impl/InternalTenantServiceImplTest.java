package com.netcracker.maas.declarative.kafka.client.impl.tenant.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.qubership.cloud.tenantmanager.client.Tenant;
import org.qubership.cloud.tenantmanager.client.TenantManagerConnector;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InternalTenantServiceImplTest {

    @Mock
    private TenantManagerConnector tenantManagerConnector;

    private InternalTenantServiceImpl tenantService;

    @BeforeEach
    void setUp() {
        tenantService = new InternalTenantServiceImpl(tenantManagerConnector);
    }

    @Test
    void listAvailableTenants_shouldReturnListOfTenantExternalIds() {
        // Arrange
        List<Tenant> tenants = createMockTenants("tenant1", "tenant2");
        when(tenantManagerConnector.getTenantList()).thenReturn(tenants);

        // Act
        List<String> result = tenantService.listAvailableTenants();

        // Assert
        assertThat(result).containsExactly("tenant1", "tenant2");
        verify(tenantManagerConnector).getTenantList();
    }

    @Test
    void listAvailableTenants_whenNoTenants_shouldReturnEmptyList() {
        // Arrange
        when(tenantManagerConnector.getTenantList()).thenReturn(Collections.emptyList());

        // Act
        List<String> result = tenantService.listAvailableTenants();

        // Assert
        assertThat(result).isEmpty();
        verify(tenantManagerConnector).getTenantList();
    }

    @Test
    void subscribe_shouldTransformAndForwardTenantUpdates() {
        // Arrange
        List<Tenant> tenants = createMockTenants("tenant1", "tenant2");
        AtomicReference<List<String>> capturedTenants = new AtomicReference<>();
        Consumer<List<String>> callback = capturedTenants::set;

        // Act
        tenantService.subscribe(callback);
        Consumer<List<Tenant>> tenantUpdateCallback = captureTenantUpdateCallback();
        tenantUpdateCallback.accept(tenants);

        // Assert
        assertThat(capturedTenants.get()).containsExactly("tenant1", "tenant2");
    }

    @Test
    void subscribe_whenEmptyTenantList_shouldForwardEmptyList() {
        // Arrange
        AtomicReference<List<String>> capturedTenants = new AtomicReference<>();
        Consumer<List<String>> callback = capturedTenants::set;

        // Act
        tenantService.subscribe(callback);
        Consumer<List<Tenant>> tenantUpdateCallback = captureTenantUpdateCallback();
        tenantUpdateCallback.accept(Collections.emptyList());

        // Assert
        assertThat(capturedTenants.get()).isEmpty();
    }

    private List<Tenant> createMockTenants(String... externalIds) {
        return Arrays.stream(externalIds)
                .map(externalId -> {
                    Tenant tenant = mock(Tenant.class);
                    when(tenant.getExternalId()).thenReturn(externalId);
                    return tenant;
                })
                .toList();
    }

    private Consumer<List<Tenant>> captureTenantUpdateCallback() {
        ArgumentCaptor<Consumer<List<Tenant>>> callbackCaptor = ArgumentCaptor.forClass(Consumer.class);
        verify(tenantManagerConnector).subscribe(callbackCaptor.capture());
        return callbackCaptor.getValue();
    }
}

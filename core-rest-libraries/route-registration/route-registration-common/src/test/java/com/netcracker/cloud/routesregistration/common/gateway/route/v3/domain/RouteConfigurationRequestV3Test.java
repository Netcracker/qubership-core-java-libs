package com.netcracker.cloud.routesregistration.common.gateway.route.v3.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RouteConfigurationRequestV3Test {

    private RouteConfigurationRequestV3 request1;
    private RouteConfigurationRequestV3 request2;
    private VirtualService vs1;
    private VirtualService vs2;
    private VirtualService vs3;

    @BeforeEach
    void setUp() {
        // Mock RouteConfig
        RouteConfig routeConfig1 = mock(RouteConfig.class);
        RouteConfig routeConfig2 = mock(RouteConfig.class);
        RouteConfig routeConfig3 = mock(RouteConfig.class);

        // Create virtual services with non-null RouteConfig
        vs1 = spy(VirtualService.builder().name("service1").routeConfiguration(routeConfig1).build());
        vs2 = spy(VirtualService.builder().name("service2").routeConfiguration(routeConfig2).build());
        vs3 = spy(VirtualService.builder().name("service3").routeConfiguration(routeConfig3).build());

        // Request 1 has vs1 and vs2
        request1 = RouteConfigurationRequestV3.builder()
                .namespace("default")
                .gateways(new ArrayList<>(List.of("gateway1")))
                .virtualServices(new ArrayList<>(List.of(vs1, vs2)))
                .build();

        // Request 2 has vs2 (overlap) and vs3 (new)
        request2 = RouteConfigurationRequestV3.builder()
                .namespace("default")
                .gateways(new ArrayList<>(List.of("gateway1")))
                .virtualServices(new ArrayList<>(List.of(vs2, vs3)))
                .build();
    }

    @Test
    void merge_shouldAddNewVirtualServices() {
        request1.merge(request2);

        assertEquals(3, request1.getVirtualServices().size(), "Should have 3 virtual services after merge");
        assertTrue(request1.getVirtualServices().stream().anyMatch(v -> v.getName().equals("service3")),
                "New virtual service vs3 should be added");
    }

    @Test
    void merge_shouldCallMergeOnOverlappingVirtualServices() {
        request1.merge(request2);

        // vs2 exists in both, merge should be called
        verify(vs2, times(1)).merge(any(VirtualService.class));

        // vs1 and vs3 are not overlapping, merge should not be called on them
        verify(vs1, never()).merge(any(VirtualService.class));
        verify(vs3, never()).merge(any(VirtualService.class));
    }

    @Test
    void merge_shouldThrowIfGatewaysDoNotMatch() {
        RouteConfigurationRequestV3 other = RouteConfigurationRequestV3.builder()
                .namespace("default")
                .gateways(List.of("differentGateway"))
                .virtualServices(List.of(vs3))
                .build();

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> request1.merge(other));

        assertTrue(ex.getMessage().contains("Both RouteConfigurationRequestV3 must have exactly one same 'gateways'"));
    }

    @Test
    void merge_shouldThrowIfMultipleGateways() {
        RouteConfigurationRequestV3 other = RouteConfigurationRequestV3.builder()
                .namespace("default")
                .gateways(List.of("gateway1", "gateway2"))
                .virtualServices(List.of(vs3))
                .build();

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> request1.merge(other));

        assertTrue(ex.getMessage().contains("Both RouteConfigurationRequestV3 must have exactly one same 'gateways'"));
    }

    @Test
    void merge_shouldReturnThis() {
        RouteConfigurationRequestV3 result = request1.merge(request2);
        assertSame(request1, result, "merge should return the original request1 instance");
    }
}

package com.netcracker.cloud.routesregistration.common.config;

import com.netcracker.cloud.routesregistration.common.gateway.route.ServiceMeshType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class RouteRegistrationProcessorConfigurationTest {

    @ParameterizedTest
    @MethodSource("provideServiceMeshTypesForPostRoutesEnabled")
    void shouldSetPostRoutesEnabledBasedOnServiceMeshType(ServiceMeshType meshType, boolean inputEnabled, boolean expectedEnabled) {
        // Given & When
        RouteRegistrationProcessorConfiguration config = new RouteRegistrationProcessorConfiguration(
                "test-service",
                "v1",
                "default",
                "/api",
                "8080",
                false,
                inputEnabled,
                Optional.ofNullable(meshType)
        );

        // Then
        assertEquals(expectedEnabled, config.getPostRoutesEnabled());
    }

    private static Stream<Arguments> provideServiceMeshTypesForPostRoutesEnabled() {
        return Stream.of(
                Arguments.of(ServiceMeshType.CORE, true, true),
                Arguments.of(ServiceMeshType.CORE, false, false),
                Arguments.of(ServiceMeshType.ISTIO, true, false),
                Arguments.of(ServiceMeshType.ISTIO, false, false),
                Arguments.of(null, true, true),
                Arguments.of(null, false, false)
        );
    }
}

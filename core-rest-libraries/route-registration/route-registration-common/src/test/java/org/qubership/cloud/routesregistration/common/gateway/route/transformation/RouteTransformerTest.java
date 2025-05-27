package org.qubership.cloud.routesregistration.common.gateway.route.transformation;

import org.junit.jupiter.api.Test;
import org.qubership.cloud.routesregistration.common.annotation.processing.ClassRoutesBuilder;
import org.qubership.cloud.routesregistration.common.annotation.processing.MicroserviceRoutesBuilder;
import org.qubership.cloud.routesregistration.common.annotation.processing.RouteAnnotationInfo;
import org.qubership.cloud.routesregistration.common.gateway.route.Constants;
import org.qubership.cloud.routesregistration.common.gateway.route.RouteEntry;
import org.qubership.cloud.routesregistration.common.gateway.route.RouteType;

import java.util.Collection;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class RouteTransformerTest {
    private static final String TEST_MICROSERVICE = "test-microservice";

    @Test
    void testInvalidRouteConfiguration() {
        RouteAnnotationInfo routeAnnotation = RouteAnnotationInfo.builder().routeType(RouteType.PRIVATE).build();

        MicroserviceRoutesBuilder microserviceRoutesBuilder = new MicroserviceRoutesBuilder();
        RouteTransformer routeTransformer = new RouteTransformer(TEST_MICROSERVICE);

        microserviceRoutesBuilder.withClass(
                new ClassRoutesBuilder(TEST_MICROSERVICE)
                        .withRouteAnnotation(routeAnnotation)
                        .withPathsTo(Collections.singleton("/test-path-1"))
                        .withGatewayPathsFrom(Collections.singleton("/test-path-from")));

        microserviceRoutesBuilder.withRoutes(
                new ClassRoutesBuilder(TEST_MICROSERVICE)
                        .withRouteAnnotation(routeAnnotation)
                        .withPathsTo(Collections.singleton("/test-path-2"))
                        .withGatewayPathsFrom(Collections.singleton("/test-path-from"))
                        .build());

        assertThrows(IllegalArgumentException.class, () -> routeTransformer.transform(microserviceRoutesBuilder.build()));
    }

    @Test
    void testAllowedRouteIsPreferred() {
        RouteAnnotationInfo privateRouteAnnotation = RouteAnnotationInfo.builder().routeType(RouteType.PRIVATE).build();
        RouteAnnotationInfo internalRouteAnnotation = RouteAnnotationInfo.builder().routeType(RouteType.INTERNAL).build();

        MicroserviceRoutesBuilder microserviceRoutesBuilder = new MicroserviceRoutesBuilder();
        RouteTransformer routeTransformer = new RouteTransformer(TEST_MICROSERVICE);

        microserviceRoutesBuilder.withClass(
                new ClassRoutesBuilder(TEST_MICROSERVICE)
                        .withRouteAnnotation(privateRouteAnnotation)
                        .withPathsTo(Collections.singleton("/test-path")));

        microserviceRoutesBuilder.withRoutes(
                new ClassRoutesBuilder(TEST_MICROSERVICE)
                        .withRouteAnnotation(internalRouteAnnotation)
                        .withPathsTo(Collections.singleton("/test-path"))
                        .build());

        Collection<RouteEntry> routes = routeTransformer.transform(microserviceRoutesBuilder.build());
        assertEquals(3, routes.size());
        assertTrue(routes.contains(RouteEntry.builder()
                .from("/test-path")
                .to("/test-path")
                .allowed(false)
                .type(RouteType.PUBLIC)
                .gateway(Constants.PRIVATE_GATEWAY_SERVICE)
                .build()));
        assertTrue(routes.contains(RouteEntry.builder()
                .from("/test-path")
                .to("/test-path")
                .allowed(true)
                .type(RouteType.PRIVATE)
                .gateway(Constants.PRIVATE_GATEWAY_SERVICE)
                .build()));
        assertTrue(routes.contains(RouteEntry.builder()
                .from("/test-path")
                .to("/test-path")
                .allowed(true)
                .type(RouteType.INTERNAL)
                .gateway(Constants.INTERNAL_GATEWAY_SERVICE)
                .build()));
    }
}

package org.qubership.cloud.routesregistration.common.annotation.processing;

import org.qubership.cloud.routesregistration.common.gateway.route.RouteEntry;
import org.qubership.cloud.routesregistration.common.gateway.route.RouteType;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collection;
import java.util.Collections;

public class MicroserviceRoutesBuilderTest {
    private static final String TEST_MICROSERVICE = "test-microservice";

    @Test
    public void testRoutesAreBuilt() {
        RouteAnnotationInfo privateRouteAnnotation = RouteAnnotationInfo.builder().routeType(RouteType.PRIVATE).build();
        RouteAnnotationInfo internalRouteAnnotation = RouteAnnotationInfo.builder().routeType(RouteType.INTERNAL).build();

        MicroserviceRoutesBuilder microserviceRoutesBuilder = new MicroserviceRoutesBuilder();

        microserviceRoutesBuilder.withClass(
                new ClassRoutesBuilder(TEST_MICROSERVICE)
                        .withRouteAnnotation(privateRouteAnnotation)
                        .withPathsTo(Collections.singleton("/test-path")));

        microserviceRoutesBuilder.withRoutes(
                new ClassRoutesBuilder(TEST_MICROSERVICE)
                        .withRouteAnnotation(internalRouteAnnotation)
                        .withPathsTo(Collections.singleton("/test-path"))
                        .build());

        Collection<RouteEntry> routes = microserviceRoutesBuilder.build();
        Assert.assertEquals(2, routes.size());
        Assert.assertTrue(routes.contains(RouteEntry.builder()
                .from("/test-path")
                .to("/test-path")
                .allowed(true)
                .type(RouteType.PRIVATE)
                .build()));
        Assert.assertTrue(routes.contains(RouteEntry.builder()
                .from("/test-path")
                .to("/test-path")
                .allowed(true)
                .type(RouteType.INTERNAL)
                .build()));
    }
}

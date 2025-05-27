package org.qubership.cloud.routesregistration.common.spring.gateway.route;

import org.junit.jupiter.api.Test;
import org.qubership.cloud.routesregistration.common.gateway.route.Constants;
import org.qubership.cloud.routesregistration.common.gateway.route.RouteEntry;
import org.qubership.cloud.routesregistration.common.gateway.route.RouteType;
import org.qubership.cloud.routesregistration.common.gateway.route.transformation.RouteTransformer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.qubership.cloud.routesregistration.common.spring.gateway.route.RouteAnnotationProcessorTest.collectionContainsExactRoute;

@SpringBootTest(classes = {RoutesTestConfiguration.class, RouteAnnotationProcessorTest.TestRegistrationConfiguration.class})
class AnnotationScanAndRotesTransformationTest {

    @Autowired
    RouteAnnotationProcessor routeAnnotationProcessor;

    @Autowired
    RouteTransformer routeTransformer;

    @Test
    void getAndTransformRouteEntries() throws Exception {
        Collection<RouteEntry> routes = new ArrayList<>();
        routes.addAll(routeAnnotationProcessor.getRouteEntries(TestController1.class));
        routes.addAll(routeAnnotationProcessor.getRouteEntries(TestController2.class));
        routes.addAll(routeAnnotationProcessor.getRouteEntries(TestController3.class));
        routes.addAll(routeAnnotationProcessor.getRouteEntries(TestController4.class));
        routes.addAll(routeAnnotationProcessor.getRouteEntries(TestController5.class));
        routes.addAll(routeAnnotationProcessor.getRouteEntries(TestController6.class));
        routes.addAll(routeAnnotationProcessor.getRouteEntries(TestControllerBaseFor6.class));
        routes.addAll(routeAnnotationProcessor.getRouteEntries(TestController7.class));
        routes.addAll(routeAnnotationProcessor.getRouteEntries(TestController8.class));
        routes.addAll(routeAnnotationProcessor.getRouteEntries(TestController10.class));
        routes.addAll(routeAnnotationProcessor.getRouteEntries(TestController12.class));
        routes.addAll(routeAnnotationProcessor.getRouteEntries(TestController13.class));
        routes.addAll(routeAnnotationProcessor.getRouteEntries(TestController15.class));
        routes.addAll(routeAnnotationProcessor.getRouteEntries(TestController16.class));
        routes.addAll(routeAnnotationProcessor.getRouteEntries(TestController17.class));

        routes = routeTransformer.transform(routes);
        validateRoutes(routes);
    }

    private void validateRoutes(Collection<RouteEntry> routes) {
        Collection<RouteEntry> expectedRoutes = enrichExpectedRoutes(RouteAnnotationProcessorTest.getExpectedRoutes());
        expectedRoutes.forEach(expectedRoute -> assertTrue(collectionContainsExactRoute(routes, expectedRoute)));
        routes.forEach(actualRoute -> assertTrue(collectionContainsExactRoute(expectedRoutes, actualRoute)));
        verifyNoDuplicates(routes);
    }

    private void verifyNoDuplicates(Collection<RouteEntry> routes) {
        List<RouteEntry> alreadyMetRoutes = new ArrayList<>(routes.size());
        routes.forEach(route -> {
            assertFalse(alreadyMetRoutes.contains(route));
            alreadyMetRoutes.add(route);
        });
    }

    private RouteEntry.RouteEntryBuilder copyRouteBuilder(RouteEntry source) {
        return RouteEntry.builder()
                .from(source.getFrom())
                .to(source.getTo())
                .namespace(source.getNamespace())
                .type(source.getType())
                .gateway(source.getGateway())
                .timeout(source.getTimeout());
    }

    private List<RouteEntry> enrichExpectedRoutes(List<RouteEntry> routes) {
        List<RouteEntry> expectedRoutes = new ArrayList<>();
        for (RouteEntry route : routes) {
            switch (route.getType()) {
                case FACADE:
                    if (route.getGateway() == null || route.getGateway().isEmpty()) {
                        route.setGateway(RoutesTestConfiguration.MICROSERVICE_TEST_NAME);
                    }
                    expectedRoutes.add(route);
                    break;
                case PUBLIC:
                    expectedRoutes.add(copyRouteBuilder(route)
                            .type(RouteType.PUBLIC)
                            .gateway(Constants.PUBLIC_GATEWAY_SERVICE)
                            .build());
                    expectedRoutes.add(copyRouteBuilder(route)
                            .type(RouteType.PUBLIC)
                            .gateway(Constants.PRIVATE_GATEWAY_SERVICE)
                            .build());
                    expectedRoutes.add(copyRouteBuilder(route)
                            .type(RouteType.PUBLIC)
                            .gateway(Constants.INTERNAL_GATEWAY_SERVICE)
                            .build());
                    break;
                case PRIVATE:
                    expectedRoutes.add(copyRouteBuilder(route)
                            .type(RouteType.PUBLIC)
                            .allowed(false)
                            .gateway(Constants.PUBLIC_GATEWAY_SERVICE)
                            .build());
                    expectedRoutes.add(copyRouteBuilder(route)
                            .type(RouteType.PUBLIC)
                            .gateway(Constants.PRIVATE_GATEWAY_SERVICE)
                            .build());
                    expectedRoutes.add(copyRouteBuilder(route)
                            .type(RouteType.PUBLIC)
                            .gateway(Constants.INTERNAL_GATEWAY_SERVICE)
                            .build());
                    break;
                case INTERNAL:
                    expectedRoutes.add(copyRouteBuilder(route)
                            .type(RouteType.PUBLIC)
                            .allowed(false)
                            .gateway(Constants.PUBLIC_GATEWAY_SERVICE)
                            .build());
                    expectedRoutes.add(copyRouteBuilder(route)
                            .type(RouteType.PUBLIC)
                            .allowed(false)
                            .gateway(Constants.PRIVATE_GATEWAY_SERVICE)
                            .build());
                    expectedRoutes.add(copyRouteBuilder(route)
                            .type(RouteType.PUBLIC)
                            .gateway(Constants.INTERNAL_GATEWAY_SERVICE)
                            .build());
                    break;
            }
        }
        return expectedRoutes;
    }
}

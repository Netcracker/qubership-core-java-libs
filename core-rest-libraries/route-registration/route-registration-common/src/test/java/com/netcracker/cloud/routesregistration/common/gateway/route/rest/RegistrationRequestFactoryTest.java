package com.netcracker.cloud.routesregistration.common.gateway.route.rest;

import org.junit.jupiter.api.Test;
import com.netcracker.cloud.routesregistration.common.gateway.route.Constants;
import com.netcracker.cloud.routesregistration.common.gateway.route.RouteEntry;
import com.netcracker.cloud.routesregistration.common.gateway.route.RouteType;
import com.netcracker.cloud.routesregistration.common.gateway.route.v3.CompositeRequestV3;
import com.netcracker.cloud.routesregistration.common.gateway.route.v3.RegistrationRequestV3;
import com.netcracker.cloud.routesregistration.common.gateway.route.v3.domain.*;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

class RegistrationRequestFactoryTest {
    static final String MICROSERVICE_URL = "http://test-ms-v1:8080";
    static final String MICROSERVICE_NAME = "test-ms";
    static final String ROOT_PATH = "/api/v1/test-ms";
    static final String ROUTE_PATH_1 = "/route1";
    static final String ROUTE_PATH_2 = "/route2";
    static final String ROUTE_PATH_3 = "/route3";
    static final String ROUTE_PATH_4 = "/route4";
    static final String ROUTE_PATH_5 = "/route5";
    static final String INGRESS_GATEWAY_FIRST = "ingress-gateway-first";
    static final String INGRESS_GATEWAY_SECOND = "ingress-gateway-second";
    static final List<String> HOSTS = Arrays.asList("host", "host2");
    static final List<String> SECOND_HOSTS = Arrays.asList("host3", "host4");

    private RegistrationRequestFactory registrationRequestFactory;

    @Test
    void createRegistrationRequests() {
        registrationRequestFactory = new RegistrationRequestFactory(
                MICROSERVICE_URL,
                MICROSERVICE_NAME,
                "v1",
                "default");
        testCreateRegistrationRequestsForNamespace("default");
    }

    @Test
    void createRegistrationRequestsInLocaldev() {
        System.setProperty("LOCALDEV_NAMESPACE", "127.0.0.1.xip.io");
        registrationRequestFactory = new RegistrationRequestFactory(
                MICROSERVICE_URL,
                MICROSERVICE_NAME,
                "v1",
                "default");
        testCreateRegistrationRequestsForNamespace("127.0.0.1.xip.io");
        System.clearProperty("LOCALDEV_NAMESPACE");
    }

    private void testCreateRegistrationRequestsForNamespace(String namespace) {
        CompositeRequest<CommonRequest> actualRequests = registrationRequestFactory.createRequests(
                buildTestRoutes(namespace), ControlPlaneApiVersion.V3);

        CompositeRequest<CommonRequest> expectedRequests = buildExpectedRegistrationRequestsV3(namespace);
        assertRequestListsEqual(expectedRequests, actualRequests);
    }

    private CompositeRequest<CommonRequest> buildExpectedRegistrationRequestsV3(String namespace) {
        RouteConfigurationRequestV3 publicGwRequest = RouteConfigurationRequestV3.builder()
                .namespace(namespace)
                .gateways(Collections.singletonList(Constants.PUBLIC_GATEWAY_SERVICE))
                .virtualServices(Collections.singletonList(
                        VirtualService.builder()
                                .name(Constants.PUBLIC_GATEWAY_SERVICE)
                                .routeConfiguration(RouteConfig.builder()
                                        .version("v1")
                                        .routes(Collections.singletonList(
                                                RouteV3.builder()
                                                        .destination(RouteDestination.builder()
                                                                .cluster(MICROSERVICE_NAME)
                                                                .endpoint(MICROSERVICE_URL)
                                                                .build())
                                                        .rules(newList(
                                                                Rule.builder()
                                                                        .allowed(true)
                                                                        .match(RouteMatch.builder().prefix(ROOT_PATH).build())
                                                                        .prefixRewrite(ROOT_PATH)
                                                                        .build(),
                                                                Rule.builder()
                                                                        .allowed(false)
                                                                        .match(RouteMatch.builder().prefix(ROOT_PATH + ROUTE_PATH_4).build())
                                                                        .prefixRewrite(ROOT_PATH + ROUTE_PATH_4)
                                                                        .build(),
                                                                Rule.builder()
                                                                        .allowed(false)
                                                                        .match(RouteMatch.builder().prefix(ROOT_PATH + ROUTE_PATH_3).build())
                                                                        .prefixRewrite(ROOT_PATH + ROUTE_PATH_3)
                                                                        .build(),
                                                                Rule.builder()
                                                                        .allowed(true)
                                                                        .match(RouteMatch.builder().prefix(ROOT_PATH + ROUTE_PATH_5).build())
                                                                        .prefixRewrite(ROOT_PATH + ROUTE_PATH_5)
                                                                        .build()
                                                        ))
                                                        .build()))
                                        .build())
                                .build()
                ))
                .build();

        RouteConfigurationRequestV3 privateGwRequest = RouteConfigurationRequestV3.builder()
                .namespace(namespace)
                .gateways(Collections.singletonList(Constants.PRIVATE_GATEWAY_SERVICE))
                .virtualServices(Collections.singletonList(
                        VirtualService.builder()
                                .name(Constants.PRIVATE_GATEWAY_SERVICE)
                                .routeConfiguration(RouteConfig.builder()
                                        .version("v1")
                                        .routes(Collections.singletonList(
                                                RouteV3.builder()
                                                        .destination(RouteDestination.builder()
                                                                .cluster(MICROSERVICE_NAME)
                                                                .endpoint(MICROSERVICE_URL)
                                                                .build())
                                                        .rules(newList(
                                                                Rule.builder()
                                                                        .allowed(true)
                                                                        .match(RouteMatch.builder().prefix(ROOT_PATH).build())
                                                                        .prefixRewrite(ROOT_PATH)
                                                                        .build(),
                                                                Rule.builder()
                                                                        .allowed(false)
                                                                        .match(RouteMatch.builder().prefix(ROOT_PATH + ROUTE_PATH_4).build())
                                                                        .prefixRewrite(ROOT_PATH + ROUTE_PATH_4)
                                                                        .build(),
                                                                Rule.builder()
                                                                        .allowed(true)
                                                                        .match(RouteMatch.builder().prefix(ROOT_PATH + ROUTE_PATH_3).build())
                                                                        .prefixRewrite(ROOT_PATH + ROUTE_PATH_3)
                                                                        .build(),
                                                                Rule.builder()
                                                                        .allowed(true)
                                                                        .match(RouteMatch.builder().prefix(ROOT_PATH + ROUTE_PATH_5).build())
                                                                        .prefixRewrite(ROOT_PATH + ROUTE_PATH_5)
                                                                        .build()
                                                        ))
                                                        .build()))
                                        .build())
                                .build()
                ))
                .build();

        RouteConfigurationRequestV3 internalGwRequest = RouteConfigurationRequestV3.builder()
                .namespace(namespace)
                .gateways(Collections.singletonList(Constants.INTERNAL_GATEWAY_SERVICE))
                .virtualServices(Collections.singletonList(
                        VirtualService.builder()
                                .name(Constants.INTERNAL_GATEWAY_SERVICE)
                                .routeConfiguration(RouteConfig.builder()
                                        .version("v1")
                                        .routes(Collections.singletonList(
                                                RouteV3.builder()
                                                        .destination(RouteDestination.builder()
                                                                .cluster(MICROSERVICE_NAME)
                                                                .endpoint(MICROSERVICE_URL)
                                                                .build())
                                                        .rules(newList(
                                                                Rule.builder()
                                                                        .allowed(true)
                                                                        .match(RouteMatch.builder().prefix(ROOT_PATH).build())
                                                                        .prefixRewrite(ROOT_PATH)
                                                                        .build(),
                                                                Rule.builder()
                                                                        .allowed(true)
                                                                        .match(RouteMatch.builder().prefix(ROOT_PATH + ROUTE_PATH_4).build())
                                                                        .prefixRewrite(ROOT_PATH + ROUTE_PATH_4)
                                                                        .build(),
                                                                Rule.builder()
                                                                        .allowed(true)
                                                                        .match(RouteMatch.builder().prefix(ROOT_PATH + ROUTE_PATH_3).build())
                                                                        .prefixRewrite(ROOT_PATH + ROUTE_PATH_3)
                                                                        .build(),
                                                                Rule.builder()
                                                                        .allowed(true)
                                                                        .match(RouteMatch.builder().prefix(ROOT_PATH + ROUTE_PATH_5).build())
                                                                        .prefixRewrite(ROOT_PATH + ROUTE_PATH_5)
                                                                        .build()
                                                        ))
                                                        .build()))
                                        .build())
                                .build()
                ))
                .build();

        RouteConfigurationRequestV3 facadeGwRequest = RouteConfigurationRequestV3.builder()
                .namespace(namespace)
                .gateways(Collections.singletonList(MICROSERVICE_NAME))
                .virtualServices(Collections.singletonList(
                        VirtualService.builder()
                                .name(MICROSERVICE_NAME)
                                .routeConfiguration(RouteConfig.builder()
                                        .version("v1")
                                        .routes(Collections.singletonList(
                                                RouteV3.builder()
                                                        .destination(RouteDestination.builder()
                                                                .cluster(MICROSERVICE_NAME)
                                                                .endpoint(MICROSERVICE_URL)
                                                                .build())
                                                        .rules(newList(
                                                                Rule.builder()
                                                                        .allowed(true)
                                                                        .match(RouteMatch.builder().prefix(ROOT_PATH).build())
                                                                        .prefixRewrite(ROOT_PATH)
                                                                        .build()
                                                        ))
                                                        .build()))
                                        .build())
                                .build()
                ))
                .build();

        RouteConfigurationRequestV3 compositeGwRequest = RouteConfigurationRequestV3.builder()
                .namespace(namespace)
                .gateways(Collections.singletonList(INGRESS_GATEWAY_FIRST))
                .virtualServices(Collections.singletonList(
                        VirtualService.builder()
                                .name(MICROSERVICE_NAME)
                                .hosts(Collections.singletonList("*"))
                                .routeConfiguration(RouteConfig.builder()
                                        .version("v1")
                                        .routes(Collections.singletonList(
                                                RouteV3.builder()
                                                        .destination(RouteDestination.builder()
                                                                .cluster(MICROSERVICE_NAME)
                                                                .endpoint(MICROSERVICE_URL)
                                                                .build())
                                                        .rules(newList(
                                                                Rule.builder()
                                                                        .allowed(true)
                                                                        .match(RouteMatch.builder().prefix(ROOT_PATH + ROUTE_PATH_1).build())
                                                                        .prefixRewrite(ROOT_PATH + ROUTE_PATH_1)
                                                                        .build(),
                                                                Rule.builder()
                                                                        .allowed(true)
                                                                        .match(RouteMatch.builder().prefix(ROOT_PATH + ROUTE_PATH_2).build())
                                                                        .prefixRewrite(ROOT_PATH + ROUTE_PATH_2)
                                                                        .build(),
                                                                Rule.builder()
                                                                        .allowed(true)
                                                                        .match(RouteMatch.builder().prefix(ROOT_PATH + ROUTE_PATH_3).build())
                                                                        .prefixRewrite(ROOT_PATH + ROUTE_PATH_3)
                                                                        .build()
                                                        ))
                                                        .build()))
                                        .build())
                                .build()
                ))
                .build();

        List<String> hosts = Stream.concat(HOSTS.stream(), SECOND_HOSTS.stream()).collect(Collectors.toList());
        RouteConfigurationRequestV3 compositeGwWithHostsRequest = RouteConfigurationRequestV3.builder()
                .namespace(namespace)
                .gateways(Collections.singletonList(INGRESS_GATEWAY_SECOND))
                .virtualServices(Collections.singletonList(
                        VirtualService.builder()
                                .name(MICROSERVICE_NAME)
                                .hosts(hosts)
                                .routeConfiguration(RouteConfig.builder()
                                        .version("v1")
                                        .routes(Collections.singletonList(
                                                RouteV3.builder()
                                                        .destination(RouteDestination.builder()
                                                                .cluster(MICROSERVICE_NAME)
                                                                .endpoint(MICROSERVICE_URL)
                                                                .build())
                                                        .rules(newList(
                                                                Rule.builder()
                                                                        .allowed(true)
                                                                        .match(RouteMatch.builder().prefix(ROOT_PATH + ROUTE_PATH_1).build())
                                                                        .prefixRewrite(ROOT_PATH + ROUTE_PATH_1)
                                                                        .build(),
                                                                Rule.builder()
                                                                        .allowed(true)
                                                                        .match(RouteMatch.builder().prefix(ROOT_PATH + ROUTE_PATH_2).build())
                                                                        .prefixRewrite(ROOT_PATH + ROUTE_PATH_2)
                                                                        .build()
                                                        ))
                                                        .build()))
                                        .build())
                                .build()
                ))
                .build();

        DeleteDomainConfigurationV3 deleteDomainConfigurationV3 = new DeleteDomainConfigurationV3(
                MICROSERVICE_NAME, INGRESS_GATEWAY_SECOND, Collections.singletonList("*")
        );

        return new CompositeRequestV3(newList(
                new RegistrationRequestV3(publicGwRequest),
                new RegistrationRequestV3(privateGwRequest),
                new RegistrationRequestV3(internalGwRequest),
                new RegistrationRequestV3(facadeGwRequest),
                new RegistrationRequestV3(compositeGwRequest),
                new RegistrationRequestV3(compositeGwWithHostsRequest)
        ), newList(deleteDomainConfigurationV3));
    }

    private static Collection<RouteEntry> buildTestRoutes(String namespace) {
        List<RouteEntry> routes = new ArrayList<>();
        routes.add(RouteEntry.builder()
                .type(RouteType.PUBLIC)
                .from(ROOT_PATH)
                .to(ROOT_PATH)
                .namespace(namespace)
                .gateway(Constants.PUBLIC_GATEWAY_SERVICE)
                .build());
        routes.add(RouteEntry.builder()
                .type(RouteType.PUBLIC)
                .from(ROOT_PATH)
                .to(ROOT_PATH)
                .namespace(namespace)
                .gateway(Constants.PRIVATE_GATEWAY_SERVICE)
                .build());
        routes.add(RouteEntry.builder()
                .type(RouteType.PUBLIC)
                .from(ROOT_PATH)
                .to(ROOT_PATH)
                .namespace(namespace)
                .gateway(Constants.INTERNAL_GATEWAY_SERVICE)
                .build());

        routes.add(RouteEntry.builder()
                .type(RouteType.INTERNAL)
                .from(ROOT_PATH + ROUTE_PATH_4)
                .to(ROOT_PATH + ROUTE_PATH_4)
                .namespace(namespace)
                .gateway(Constants.INTERNAL_GATEWAY_SERVICE)
                .build());
        routes.add(RouteEntry.builder()
                .type(RouteType.INTERNAL)
                .from(ROOT_PATH + ROUTE_PATH_4)
                .to(ROOT_PATH + ROUTE_PATH_4)
                .namespace(namespace)
                .gateway(Constants.PUBLIC_GATEWAY_SERVICE)
                .allowed(false)
                .build());
        routes.add(RouteEntry.builder()
                .type(RouteType.INTERNAL)
                .from(ROOT_PATH + ROUTE_PATH_4)
                .to(ROOT_PATH + ROUTE_PATH_4)
                .namespace(namespace)
                .gateway(Constants.PRIVATE_GATEWAY_SERVICE)
                .allowed(false)
                .build());

        routes.add(RouteEntry.builder()
                .type(RouteType.FACADE)
                .from(ROOT_PATH)
                .to(ROOT_PATH)
                .namespace(namespace)
                .gateway(null)
                .build());
        routes.add(RouteEntry.builder()
                .type(RouteType.FACADE)
                .from(ROOT_PATH + ROUTE_PATH_1)
                .to(ROOT_PATH + ROUTE_PATH_1)
                .namespace(namespace)
                .gateway(INGRESS_GATEWAY_FIRST)
                .build());
        routes.add(RouteEntry.builder()
                .type(RouteType.FACADE)
                .from(ROOT_PATH + ROUTE_PATH_2)
                .to(ROOT_PATH + ROUTE_PATH_2)
                .namespace(namespace)
                .gateway(INGRESS_GATEWAY_FIRST)
                .build());
        routes.add(RouteEntry.builder()
                .type(RouteType.FACADE)
                .from(ROOT_PATH + ROUTE_PATH_3)
                .to(ROOT_PATH + ROUTE_PATH_3)
                .namespace(namespace)
                .gateway(INGRESS_GATEWAY_FIRST)
                .build());

        routes.add(RouteEntry.builder()
                .type(RouteType.FACADE)
                .from(ROOT_PATH + ROUTE_PATH_1)
                .to(ROOT_PATH + ROUTE_PATH_1)
                .namespace(namespace)
                .hosts(HOSTS.stream().sorted().collect(Collectors.toCollection(LinkedHashSet::new)))
                .gateway(INGRESS_GATEWAY_SECOND)
                .build());
        routes.add(RouteEntry.builder()
                .type(RouteType.FACADE)
                .from(ROOT_PATH + ROUTE_PATH_2)
                .to(ROOT_PATH + ROUTE_PATH_2)
                .namespace(namespace)
                .hosts(SECOND_HOSTS.stream().sorted().collect(Collectors.toCollection(LinkedHashSet::new)))
                .gateway(INGRESS_GATEWAY_SECOND)
                .build());

        routes.add(RouteEntry.builder()
                .type(RouteType.PRIVATE)
                .from(ROOT_PATH + ROUTE_PATH_3)
                .to(ROOT_PATH + ROUTE_PATH_3)
                .namespace(namespace)
                .gateway(Constants.PRIVATE_GATEWAY_SERVICE)
                .build());
        routes.add(RouteEntry.builder()
                .type(RouteType.PRIVATE)
                .from(ROOT_PATH + ROUTE_PATH_3)
                .to(ROOT_PATH + ROUTE_PATH_3)
                .namespace(namespace)
                .gateway(Constants.PUBLIC_GATEWAY_SERVICE)
                .allowed(false)
                .build());
        routes.add(RouteEntry.builder()
                .type(RouteType.PRIVATE)
                .from(ROOT_PATH + ROUTE_PATH_3)
                .to(ROOT_PATH + ROUTE_PATH_3)
                .namespace(namespace)
                .gateway(Constants.INTERNAL_GATEWAY_SERVICE)
                .build());

        routes.add(RouteEntry.builder()
                .type(RouteType.PUBLIC)
                .from(ROOT_PATH + ROUTE_PATH_5)
                .to(ROOT_PATH + ROUTE_PATH_5)
                .namespace(namespace)
                .gateway(Constants.PUBLIC_GATEWAY_SERVICE)
                .build());
        routes.add(RouteEntry.builder()
                .type(RouteType.PUBLIC)
                .from(ROOT_PATH + ROUTE_PATH_5)
                .to(ROOT_PATH + ROUTE_PATH_5)
                .namespace(namespace)
                .gateway(Constants.PRIVATE_GATEWAY_SERVICE)
                .build());
        routes.add(RouteEntry.builder()
                .type(RouteType.PUBLIC)
                .from(ROOT_PATH + ROUTE_PATH_5)
                .to(ROOT_PATH + ROUTE_PATH_5)
                .namespace(namespace)
                .gateway(Constants.INTERNAL_GATEWAY_SERVICE)
                .build());
        return routes;
    }

    private static void assertRequestListsEqual(CompositeRequest<CommonRequest> expectedRequests, CompositeRequest<CommonRequest> actualRequests) {
        expectedRequests.forEach(expectedRequest -> assertTrue(containsRequest(actualRequests, expectedRequest)));
        actualRequests.forEach(actualRequest -> assertTrue(containsRequest(expectedRequests, actualRequest)));
    }

    private static boolean containsRequest(CompositeRequest<CommonRequest> requests, CommonRequest request) {
        for (CommonRequest actualRequest : requests) {
            if (actualRequest.getPayload().equals(request.getPayload())) {
                return true;
            }
        }
        return false;
    }

    static <T> List<T> newList(T... values) {
        final List<T> result = new ArrayList<>(values.length);
        Collections.addAll(result, values);
        return result;
    }
}

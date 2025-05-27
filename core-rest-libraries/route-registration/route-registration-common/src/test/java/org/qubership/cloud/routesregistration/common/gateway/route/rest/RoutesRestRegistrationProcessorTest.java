package org.qubership.cloud.routesregistration.common.gateway.route.rest;

import io.reactivex.schedulers.Schedulers;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.qubership.cloud.routesregistration.common.gateway.route.*;
import org.qubership.cloud.routesregistration.common.gateway.route.transformation.RouteTransformer;
import org.qubership.cloud.routesregistration.common.gateway.route.v3.CompositeRequestV3;
import org.qubership.cloud.routesregistration.common.gateway.route.v3.RegistrationRequestV3;
import org.qubership.cloud.routesregistration.common.gateway.route.v3.domain.*;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.qubership.cloud.routesregistration.common.gateway.route.rest.RegistrationRequestFactoryTest.*;

class RoutesRestRegistrationProcessorTest {

    @Test
    void postRoutes() throws InterruptedException {
        RouteRetryManager retryManager = new RouteRetryManager(Schedulers.computation(), new RoutesRegistrationDelayProvider());
        ControlPlaneClient controlPlaneClient = Mockito.mock(ControlPlaneClient.class);
        RouteTransformer routeTransformer = new RouteTransformer(MICROSERVICE_NAME);
        RegistrationRequestFactory registrationRequestFactory = new RegistrationRequestFactory(
                MICROSERVICE_URL,
                MICROSERVICE_NAME,
                "v1",
                "default");
        RoutesRestRegistrationProcessor processor = new RoutesRestRegistrationProcessor(
                controlPlaneClient,
                retryManager,
                routeTransformer,
                registrationRequestFactory,
                true,
                MICROSERVICE_NAME,
                MICROSERVICE_URL);

        final CompositeRequest<CommonRequest> expectedRequests = buildExpectedRegistrationRequestsV3("default");

        final CountDownLatch countDownLatch = new CountDownLatch(7);
        Mockito.doAnswer(invocationOnMock -> {
            countDownLatch.countDown();
            return null;
        }).when(controlPlaneClient).sendRequest(any());

        processor.postRoutes(buildTestRoutes("default"));

        assertTrue(countDownLatch.await(2, TimeUnit.MINUTES));
        expectedRequests.forEach(request ->
                Mockito.verify(controlPlaneClient, times(1))
                        .sendRequest(request));
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
                .type(RouteType.INTERNAL)
                .from(ROOT_PATH + ROUTE_PATH_4)
                .to(ROOT_PATH + ROUTE_PATH_4)
                .namespace(namespace)
                .gateway(Constants.INTERNAL_GATEWAY_SERVICE)
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
                .type(RouteType.PRIVATE)
                .from(ROOT_PATH + ROUTE_PATH_3)
                .to(ROOT_PATH + ROUTE_PATH_3)
                .namespace(namespace)
                .gateway(Constants.PRIVATE_GATEWAY_SERVICE)
                .build());

        routes.add(RouteEntry.builder()
                .type(RouteType.PUBLIC)
                .from(ROOT_PATH + ROUTE_PATH_5)
                .to(ROOT_PATH + ROUTE_PATH_5)
                .namespace(namespace)
                .gateway(Constants.PUBLIC_GATEWAY_SERVICE)
                .build());
        return routes;
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

        RouteConfigurationRequestV3 compositeGwWithHostsRequest = RouteConfigurationRequestV3.builder()
                .namespace(namespace)
                .gateways(Collections.singletonList(INGRESS_GATEWAY_SECOND))
                .virtualServices(Collections.singletonList(
                        VirtualService.builder()
                                .name(MICROSERVICE_NAME)
                                .hosts(HOSTS)
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
}

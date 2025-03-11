package org.qubership.cloud.quarkus.routesregistration.deployment;

import org.qubership.cloud.quarkus.routesregistration.deployment.config.RequestRecorder;
import org.qubership.cloud.quarkus.routesregistration.deployment.config.TestConfig;
import org.qubership.cloud.quarkus.routesregistration.deployment.resource.CustomerResourceWithCustomRoute;
import org.qubership.cloud.routesregistration.common.annotation.Route;
import org.qubership.cloud.routesregistration.common.gateway.route.Constants;
import org.qubership.cloud.routesregistration.common.gateway.route.rest.CommonRequest;
import org.qubership.cloud.routesregistration.common.gateway.route.rest.CompositeRequest;
import org.qubership.cloud.routesregistration.common.gateway.route.rest.RegistrationRequest;
import org.qubership.cloud.routesregistration.common.gateway.route.v3.CompositeRequestV3;
import org.qubership.cloud.routesregistration.common.gateway.route.v3.RegistrationRequestV3;
import org.qubership.cloud.routesregistration.common.gateway.route.v3.domain.*;
import io.quarkus.test.QuarkusUnitTest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.*;

public class GatewayCustomRoutesRegistrationProcessorTest extends AbstractRoutesRegistrationProcessorTest {
    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClass(TestConfig.class)
                    .addClass(RoutesRegistrationProcessor.class)
                    .addClass(CustomerResourceWithCustomRoute.class)
                    .addClass(RequestRecorder.class)
                    .addClass(Route.class)
                    .addAsResource("application.properties")
            );

    @Test
    public void mustOverrideGatewayRouteByMethodAnnotation() {
        Set<RouteConfigurationRequestV3> v3Requests = retrieveReqV3Body();
        CompositeRequest<CommonRequest> expectedRequests = buildExpectedRegistrationRequestsV3(cloudNamespace);
        assertRequestEquals(expectedRequests, v3Requests);
    }

    private CompositeRequest<CommonRequest> buildExpectedRegistrationRequestsV3(String namespace) {
        List<RegistrationRequest> registrationRequests = new ArrayList<>();

        registrationRequests.add(new RegistrationRequestV3(RouteConfigurationRequestV3.builder()
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
                                                                .cluster(microserviceName)
                                                                .endpoint("http://quarkus-quickstart-test-v1:8080")
                                                                .build())
                                                        .rules(Arrays.asList(
                                                                Rule.builder()
                                                                        .allowed(false)
                                                                        .match(RouteMatch.builder().prefix("/gateway-custom-route-test/gateway-private").build())
                                                                        .prefixRewrite("/custom-route-test/private")
                                                                        .build(),
                                                                Rule.builder()
                                                                        .allowed(true)
                                                                        .match(RouteMatch.builder().prefix("/gateway-custom-route-test").build())
                                                                        .prefixRewrite("/custom-route-test")
                                                                        .build()
                                                        ))
                                                        .build()))
                                        .build())
                                .build()
                ))
                .build()
        ));

        registrationRequests.add(new RegistrationRequestV3(RouteConfigurationRequestV3.builder()
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
                                                                .cluster(microserviceName)
                                                                .endpoint("http://quarkus-quickstart-test-v1:8080")
                                                                .build())
                                                        .rules(Arrays.asList(
                                                                Rule.builder()
                                                                        .allowed(true)
                                                                        .match(RouteMatch.builder().prefix("/gateway-custom-route-test/gateway-private").build())
                                                                        .prefixRewrite("/custom-route-test/private")
                                                                        .build(),
                                                                Rule.builder()
                                                                        .allowed(true)
                                                                        .match(RouteMatch.builder().prefix("/gateway-custom-route-test").build())
                                                                        .prefixRewrite("/custom-route-test")
                                                                        .build()
                                                        ))
                                                        .build()))
                                        .build())
                                .build()
                ))
                .build()
        ));

        registrationRequests.add(new RegistrationRequestV3(RouteConfigurationRequestV3.builder()
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
                                                                .cluster(microserviceName)
                                                                .endpoint("http://quarkus-quickstart-test-v1:8080")
                                                                .build())
                                                        .rules(Arrays.asList(
                                                                Rule.builder()
                                                                        .allowed(true)
                                                                        .match(RouteMatch.builder().prefix("/gateway-custom-route-test/gateway-private").build())
                                                                        .prefixRewrite("/custom-route-test/private")
                                                                        .build(),
                                                                Rule.builder()
                                                                        .allowed(true)
                                                                        .match(RouteMatch.builder().prefix("/gateway-custom-route-test").build())
                                                                        .prefixRewrite("/custom-route-test")
                                                                        .build()
                                                        ))
                                                        .build()))
                                        .build())
                                .build()
                ))
                .build()
        ));

        registrationRequests.add(new RegistrationRequestV3(RouteConfigurationRequestV3.builder()
                .namespace(namespace)
                .gateways(Collections.singletonList(microserviceName))
                .virtualServices(Collections.singletonList(
                        VirtualService.builder()
                                .name(microserviceName)
                                .routeConfiguration(RouteConfig.builder()
                                        .version("v1")
                                        .routes(Collections.singletonList(
                                                RouteV3.builder()
                                                        .destination(RouteDestination.builder()
                                                                .cluster(microserviceName)
                                                                .endpoint("http://quarkus-quickstart-test-v1:8080")
                                                                .build())
                                                        .rules(Arrays.asList(
                                                                Rule.builder()
                                                                        .allowed(true)
                                                                        .match(RouteMatch.builder().prefix("/custom-route-test").build())
                                                                        .prefixRewrite("/custom-route-test")
                                                                        .build(),
                                                                Rule.builder()
                                                                        .allowed(true)
                                                                        .match(RouteMatch.builder().prefix("/custom-route-test/facade-route").build())
                                                                        .prefixRewrite("/custom-route-test/facade")
                                                                        .build()
                                                        ))
                                                        .build()))
                                        .build())
                                .build()
                ))
                .build()
        ));

        return new CompositeRequestV3(registrationRequests, Collections.emptyList());
    }
}

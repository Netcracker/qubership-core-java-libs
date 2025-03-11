package org.qubership.cloud.quarkus.routesregistration.deployment;

import org.qubership.cloud.quarkus.routesregistration.deployment.config.RequestRecorder;
import org.qubership.cloud.quarkus.routesregistration.deployment.config.TestConfig;
import org.qubership.cloud.quarkus.routesregistration.deployment.resource.CustomGatewayNamesResource;
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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class CustomGatewayNamesTest extends AbstractRoutesRegistrationProcessorTest {
    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClass(CustomGatewayNamesResource.class)
                    .addClass(TestConfig.class)
                    .addClass(RoutesRegistrationProcessor.class)
                    .addClass(RequestRecorder.class)
                    .addAsResource("application.properties")
            );

    @Test
    public void mustBuildV2AndV3Requests() {
        Set<RouteConfigurationRequestV3> v3Requests = retrieveReqV3Body();
        assertNotNull(v3Requests);
        assertFalse(v3Requests.isEmpty());
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
                                                                        .match(RouteMatch.builder().prefix("/api/v1/test-ms/test-controller").build())
                                                                        .prefixRewrite("/test-controller")
                                                                        .build(),
                                                                Rule.builder()
                                                                        .allowed(false)
                                                                        .match(RouteMatch.builder().prefix("/api/v1/test-ms/test-controller/second").build())
                                                                        .prefixRewrite("/test-controller/second")
                                                                        .build(),
                                                                Rule.builder()
                                                                        .allowed(false)
                                                                        .match(RouteMatch.builder().prefix("/api/v1/test-ms/test-controller/third").build())
                                                                        .prefixRewrite("/test-controller/third")
                                                                        .build(),
                                                                Rule.builder()
                                                                        .allowed(false)
                                                                        .match(RouteMatch.builder().prefix("/api/v1/test-ms/test-controller/fourth").build())
                                                                        .prefixRewrite("/test-controller/fourth")
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
                                                                        .allowed(false)
                                                                        .match(RouteMatch.builder().prefix("/api/v1/test-ms/test-controller").build())
                                                                        .prefixRewrite("/test-controller")
                                                                        .build(),
                                                                Rule.builder()
                                                                        .allowed(true)
                                                                        .match(RouteMatch.builder().prefix("/api/v1/test-ms/test-controller/second").build())
                                                                        .prefixRewrite("/test-controller/second")
                                                                        .build(),
                                                                Rule.builder()
                                                                        .allowed(true)
                                                                        .match(RouteMatch.builder().prefix("/api/v1/test-ms/test-controller/third").build())
                                                                        .prefixRewrite("/test-controller/third")
                                                                        .build(),
                                                                Rule.builder()
                                                                        .allowed(true)
                                                                        .match(RouteMatch.builder().prefix("/api/v1/test-ms/test-controller/fourth").build())
                                                                        .prefixRewrite("/test-controller/fourth")
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
                                                                        .match(RouteMatch.builder().prefix("/api/v1/test-ms/test-controller").build())
                                                                        .prefixRewrite("/test-controller")
                                                                        .build(),
                                                                Rule.builder()
                                                                        .allowed(true)
                                                                        .match(RouteMatch.builder().prefix("/api/v1/test-ms/test-controller/second").build())
                                                                        .prefixRewrite("/test-controller/second")
                                                                        .build(),
                                                                Rule.builder()
                                                                        .allowed(true)
                                                                        .match(RouteMatch.builder().prefix("/api/v1/test-ms/test-controller/third").build())
                                                                        .prefixRewrite("/test-controller/third")
                                                                        .build(),
                                                                Rule.builder()
                                                                        .allowed(true)
                                                                        .match(RouteMatch.builder().prefix("/api/v1/test-ms/test-controller/fourth").build())
                                                                        .prefixRewrite("/test-controller/fourth")
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
                                                                        .match(RouteMatch.builder().prefix("/api/v1/test-controller/third").build())
                                                                        .prefixRewrite("/test-controller/third")
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
                .gateways(Collections.singletonList("composite-gateway"))
                .virtualServices(Collections.singletonList(
                        VirtualService.builder()
                                .name(microserviceName)
                                .hosts(Collections.singletonList("*"))
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
                                                                        .match(RouteMatch.builder().prefix("/api/v1/test-ms/test-controller/fourth").build())
                                                                        .prefixRewrite("/test-controller/fourth")
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

package org.qubership.cloud.routesregistration.common.spring.gateway.route;

import io.reactivex.Scheduler;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.qubership.cloud.restclient.MicroserviceRestClient;
import org.qubership.cloud.routesregistration.common.gateway.route.ControlPlaneClient;
import org.qubership.cloud.routesregistration.common.gateway.route.RouteEntry;
import org.qubership.cloud.routesregistration.common.gateway.route.RouteRetryManager;
import org.qubership.cloud.routesregistration.common.gateway.route.RoutesRestRegistrationProcessor;
import org.qubership.cloud.routesregistration.common.gateway.route.rest.RegistrationRequestFactory;
import org.qubership.cloud.routesregistration.common.gateway.route.transformation.RouteTransformer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;


@SpringBootTest(classes = RoutesTestConfigurationWithoutDefaultMapping.class,
        properties = {"apigateway.routes.registration.enabled=true"})
class RegistrationRequestV3FactoryTest {

    private static final String LOCALDEV_NAMESPACE_ENV = "LOCALDEV_NAMESPACE";
    private static final String DEFAULT_DEPLOYMENT_VERSION = "v1";

    private static final String LOCALDEV_NAMESPACE = "127.0.0.1.xip.io";
    private static final String APP_NAME = "name";
    private static final String CLOUD_NAMESPACE = "default";

    private RoutesRestRegistrationProcessor routesRestRegistrationProcessor;
    @Autowired
    private RouteAnnotationProcessor routeAnnotationProcessor;
    @Autowired
    private Scheduler rxScheduler;
    @Autowired
    private DefaultListableBeanFactory beanFactory;
    @Autowired
    private RouteRetryManager routeRetryManager;
    @Autowired
    private RouteTransformer routeTransformer;
    @Autowired
    private MicroserviceRestClient microserviceRestClient;
    public static MockWebServer server;

    /**
     * This method is used for context setups, that cannot be done
     * in {@link RoutesRegistrationTest#setUp()} method, since these setups
     * depend on System properties, which are being initialized in test methods.
     *
     * @param namespace Namespace to be tested in current test case (current test method).
     */
    private void resetTestContext(String namespace, String version) {
        String microserviceAddress = "http://" + APP_NAME + "-" + version + ":8080";
        RegistrationRequestFactory registrationRequestFactory = new RegistrationRequestFactory(microserviceAddress, APP_NAME, version, namespace);
        ControlPlaneClient controlPlaneClient = new SpringControlPlaneClient(server.url("/").toString(), microserviceRestClient);
        routesRestRegistrationProcessor = new RoutesRestRegistrationProcessor(controlPlaneClient,
                routeRetryManager,
                routeTransformer,
                registrationRequestFactory,
                true,
                RoutesTestConfiguration.MICROSERVICE_TEST_NAME,
                RoutesTestConfiguration.MICROSERVICE_TEST_NAME + ":" + RoutesTestConfiguration.PORT + RoutesTestConfiguration.CONTEXT_PATH
        );
        beanFactory.autowireBean(routesRestRegistrationProcessor);
    }

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        rxScheduler.start();
    }

    @Test
    void testRoutesValidateAnyHosts() throws Exception {
        System.setProperty(LOCALDEV_NAMESPACE_ENV, LOCALDEV_NAMESPACE);
        resetTestContext(CLOUD_NAMESPACE, DEFAULT_DEPLOYMENT_VERSION);

        List<RouteEntry> routes = routeAnnotationProcessor.getRouteEntries(TestController19.class);
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> routesRestRegistrationProcessor.postRoutes(routes)
        );
        assertEquals("Route cannot have virtual host \"*\" because there is at least one another route with different virtual host", exception.getMessage());
    }

    @Test
    void testRoutesValidateEmptyHosts() throws Exception {
        System.setProperty(LOCALDEV_NAMESPACE_ENV, LOCALDEV_NAMESPACE);
        resetTestContext(CLOUD_NAMESPACE, DEFAULT_DEPLOYMENT_VERSION);

        List<RouteEntry> routes = routeAnnotationProcessor.getRouteEntries(TestController20.class);
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> routesRestRegistrationProcessor.postRoutes(routes)
        );
        assertEquals("Route cannot have empty virtual host because there is at least one another route with different virtual host", exception.getMessage());
    }

    @Test
    void testRoutesValidateEmptyAndAnyHosts() throws Exception {
        System.setProperty(LOCALDEV_NAMESPACE_ENV, LOCALDEV_NAMESPACE);
        resetTestContext(CLOUD_NAMESPACE, DEFAULT_DEPLOYMENT_VERSION);

        List<RouteEntry> routes = routeAnnotationProcessor.getRouteEntries(TestController21.class);
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> routesRestRegistrationProcessor.postRoutes(routes)
        );
        assertEquals("Route cannot have empty virtual host because there is at least one another route with different virtual host", exception.getMessage());
    }
}

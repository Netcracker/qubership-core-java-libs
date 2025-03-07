package org.qubership.cloud.routesregistration.common.spring.gateway.route;

import org.qubership.cloud.restclient.MicroserviceRestClient;
import org.qubership.cloud.routesregistration.common.gateway.route.ControlPlaneClient;
import org.qubership.cloud.routesregistration.common.gateway.route.RouteEntry;
import org.qubership.cloud.routesregistration.common.gateway.route.RouteRetryManager;
import org.qubership.cloud.routesregistration.common.gateway.route.RoutesRestRegistrationProcessor;
import org.qubership.cloud.routesregistration.common.gateway.route.rest.RegistrationRequestFactory;
import org.qubership.cloud.routesregistration.common.gateway.route.transformation.RouteTransformer;
import io.reactivex.Scheduler;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = RoutesTestConfigurationWithoutDefaultMapping.class,
        properties = {"apigateway.routes.registration.enabled=true"})
public class RegistrationRequestV3FactoryTest {

    @Rule
    public final Timeout TEST_TIMEOUT = Timeout.seconds(1000000);

    private static final String LOCALDEV_NAMESPACE_ENV = "LOCALDEV_NAMESPACE";
    private static final String DEFAULT_DEPLOYMENT_VERSION = "v1";
    private static final String V2_DEPLOYMENT_VERSION = "v2";

    private static final String LOCALDEV_NAMESPACE = "127.0.0.1.xip.io";
    private static final String APP_NAME = "name";
    private static final String CLOUD_NAMESPACE = "default";
    private static final Class<TestController2> BEAN_CLASS = TestController2.class;

    private static final int TEST_REGISTRATIONS_NUM = 3;
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

    @Before
    public void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        rxScheduler.start();
    }

    @Test
    public void testRoutesValidateAnyHosts() throws Exception {
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
    public void testRoutesValidateEmptyHosts() throws Exception {
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
    public void testRoutesValidateEmptyAndAnyHosts() throws Exception {
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

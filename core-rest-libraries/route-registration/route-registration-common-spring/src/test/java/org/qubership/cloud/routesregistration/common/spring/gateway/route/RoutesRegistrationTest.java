package org.qubership.cloud.routesregistration.common.spring.gateway.route;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.reactivex.Scheduler;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.qubership.cloud.restclient.MicroserviceRestClient;
import org.qubership.cloud.routesregistration.common.gateway.route.ControlPlaneClient;
import org.qubership.cloud.routesregistration.common.gateway.route.RouteRetryManager;
import org.qubership.cloud.routesregistration.common.gateway.route.RoutesRestRegistrationProcessor;
import org.qubership.cloud.routesregistration.common.gateway.route.rest.RegistrationRequestFactory;
import org.qubership.cloud.routesregistration.common.gateway.route.transformation.RouteTransformer;
import org.qubership.cloud.routesregistration.common.gateway.route.v3.domain.RouteConfigurationRequestV3;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


@SpringBootTest(classes = RoutesTestConfiguration.class,
        properties = {"apigateway.routes.registration.enabled=true"})
class RoutesRegistrationTest {

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

    private void verifyRequest(RouteConfigurationRequestV3 requestEntity, String expectedNamespace, String expectedVersion) {
        assertNotNull(requestEntity);
        assertEquals(expectedNamespace, requestEntity.getNamespace());
        assertNotNull(requestEntity.getVirtualServices());
        assertEquals(1, requestEntity.getVirtualServices().size());
        assertEquals(expectedVersion, requestEntity.getVirtualServices().get(0).getRouteConfiguration().getVersion());
    }

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        rxScheduler.start();
    }

    @Test
    void testRoutesNamespaceInLocalDev() throws Exception {
        System.setProperty(LOCALDEV_NAMESPACE_ENV, LOCALDEV_NAMESPACE);
        resetTestContext(CLOUD_NAMESPACE, DEFAULT_DEPLOYMENT_VERSION);

        routesRestRegistrationProcessor.postRoutes(routeAnnotationProcessor.getRouteEntries(BEAN_CLASS));

        for (int i = 0; i < TEST_REGISTRATIONS_NUM; i++) {
            server.enqueue(new MockResponse().setResponseCode(201));
        }

        for (int i = 0; i < TEST_REGISTRATIONS_NUM; i++) {
            RouteConfigurationRequestV3 requestEntity = new ObjectMapper().readValue(server.takeRequest(2, TimeUnit.MINUTES).getBody().readUtf8(), new TypeReference<RouteConfigurationRequestV3>() {
            });
            verifyRequest(requestEntity, LOCALDEV_NAMESPACE, DEFAULT_DEPLOYMENT_VERSION);
        }
    }

    @Test
    void testRoutesNamespaceInCloud() throws Exception {
        System.clearProperty(LOCALDEV_NAMESPACE_ENV);
        resetTestContext(CLOUD_NAMESPACE, DEFAULT_DEPLOYMENT_VERSION);

        routesRestRegistrationProcessor.postRoutes(routeAnnotationProcessor.getRouteEntries(BEAN_CLASS));

        for (int i = 0; i < TEST_REGISTRATIONS_NUM; i++) {
            server.enqueue(new MockResponse().setResponseCode(201));
        }

        for (int i = 0; i < TEST_REGISTRATIONS_NUM; i++) {
            RouteConfigurationRequestV3 requestEntity = new ObjectMapper().readValue(server.takeRequest(2, TimeUnit.MINUTES).getBody().readUtf8(), new TypeReference<RouteConfigurationRequestV3>() {
            });
            verifyRequest(requestEntity, CLOUD_NAMESPACE, DEFAULT_DEPLOYMENT_VERSION);
        }
    }


    @Test
    void testVersionedRoutesRegistration() throws Exception {
        System.clearProperty(LOCALDEV_NAMESPACE_ENV);
        resetTestContext(CLOUD_NAMESPACE, V2_DEPLOYMENT_VERSION);

        routesRestRegistrationProcessor.postRoutes(routeAnnotationProcessor.getRouteEntries(BEAN_CLASS));

        for (int i = 0; i < TEST_REGISTRATIONS_NUM; i++) {
            server.enqueue(new MockResponse().setResponseCode(201));
        }

        for (int i = 0; i < TEST_REGISTRATIONS_NUM; i++) {
            RouteConfigurationRequestV3 requestEntity = new ObjectMapper().readValue(server.takeRequest(2, TimeUnit.MINUTES).getBody().readUtf8(), new TypeReference<RouteConfigurationRequestV3>() {
            });
            verifyRequest(requestEntity, CLOUD_NAMESPACE, V2_DEPLOYMENT_VERSION);
        }
    }

    @AfterEach
    void tearDown() {
        rxScheduler.shutdown();
    }
}

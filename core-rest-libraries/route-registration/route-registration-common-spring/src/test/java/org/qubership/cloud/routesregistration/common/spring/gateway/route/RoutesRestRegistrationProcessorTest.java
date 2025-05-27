package org.qubership.cloud.routesregistration.common.spring.gateway.route;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.reactivex.Scheduler;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.qubership.cloud.restclient.MicroserviceRestClient;
import org.qubership.cloud.routesregistration.common.gateway.route.ControlPlaneClient;
import org.qubership.cloud.routesregistration.common.gateway.route.RouteEntry;
import org.qubership.cloud.routesregistration.common.gateway.route.RouteRetryManager;
import org.qubership.cloud.routesregistration.common.gateway.route.RoutesRestRegistrationProcessor;
import org.qubership.cloud.routesregistration.common.gateway.route.rest.RegistrationRequestFactory;
import org.qubership.cloud.routesregistration.common.gateway.route.transformation.RouteTransformer;
import org.qubership.cloud.routesregistration.common.gateway.route.v3.domain.RouteConfigurationRequestV3;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = {
        RoutesTestConfiguration.class,
        RoutesRestRegistrationProcessorTest.TestRegistrationConfiguration.class},
        properties = {"apigateway.routes.registration.enabled=true"})
class RoutesRestRegistrationProcessorTest {

    private RoutesRestRegistrationProcessor routesRestRegistrationProcessor;
    @Autowired
    private RouteAnnotationProcessor routeAnnotationProcessor;
    @Autowired
    private RouteRetryManager routeRetryManager;
    @Autowired
    private RouteTransformer routeTransformer;
    @Autowired
    private RegistrationRequestFactory registrationRequestFactory;
    @Autowired
    private MicroserviceRestClient microserviceRestClient;
    @Autowired
    private Scheduler rxScheduler;
    @Autowired
    private DefaultListableBeanFactory beanFactory;

    private ObjectMapper objectMapper = new ObjectMapper();

    public MockWebServer server;

    @Value("${cloud.microservice.name}")
    private String serviceName;
    /**
     * Number of executing POST HTTP requests for one
     * {@link RoutesRestRegistrationProcessor#postRoutes}
     * method call.
     */
    private static final int POST_ROUTES_CALLS_NUMBER = 3;

    @BeforeEach
    void setUp() throws Exception {
        rxScheduler.start();
        server = new MockWebServer();
        server.start();
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

    @AfterEach
    void tearDown() throws IOException {
        rxScheduler.shutdown();
        server.shutdown();
    }

    @Test
    void postRoutes() throws Exception {
        Collection<RouteEntry> routes = routeAnnotationProcessor.scanForRoutes();

        startRoutesPostingThreads(routes);

        int postRequestsCount = POST_ROUTES_CALLS_NUMBER * RoutesTestConfiguration.THREADS_NUM;

        for (int i = 0; i < postRequestsCount; i++) {
            server.enqueue(new MockResponse().setResponseCode(201));
        }
        List<RecordedRequest> recordedRequests = new ArrayList<>(server.getRequestCount());
        for (int i = 0; i < postRequestsCount; i++) {
            recordedRequests.add(server.takeRequest(2, TimeUnit.MINUTES));
        }
        assertEquals(postRequestsCount, server.getRequestCount());

        List<RouteConfigurationRequestV3> recordedRequestBodies = readRequests(recordedRequests);

        List<RouteConfigurationRequestV3> publicGroup = recordedRequestBodies.stream()
                .filter(recordedRequest -> recordedRequest.getGateways().get(0).equals(RoutesTestConfiguration.PUBLIC_NODE_GROUP))
                .collect(Collectors.toList());
        List<RouteConfigurationRequestV3> privateGroup = recordedRequestBodies.stream()
                .filter(recordedRequest -> recordedRequest.getGateways().get(0).equals(RoutesTestConfiguration.PRIVATE_NODE_GROUP))
                .collect(Collectors.toList());
        List<RouteConfigurationRequestV3> internalGroup = recordedRequestBodies.stream()
                .filter(recordedRequest -> recordedRequest.getGateways().get(0).equals(RoutesTestConfiguration.INTERNAL_NODE_GROUP))
                .collect(Collectors.toList());
        assertTrue(publicGroup.size() > 2);
        assertTrue(privateGroup.size() > 2);
        assertTrue(internalGroup.size() > 2);
    }

    private List<RouteConfigurationRequestV3> readRequests(List<RecordedRequest> requests) {
        return requests.stream()
                .map(recordedRequest -> {
                    try {
                        return objectMapper.readValue(recordedRequest.getBody().readUtf8(), new TypeReference<RouteConfigurationRequestV3>() {
                        });
                    } catch (JsonProcessingException e) {
                        throw new IllegalArgumentException("Error parsing request body");
                    }
                })
                .collect(Collectors.toList());
    }

    private void startRoutesPostingThreads(Collection<RouteEntry> routes) {
        IntStream.rangeClosed(1, RoutesTestConfiguration.THREADS_NUM)
                .forEach(i -> {
                            try {
                                routesRestRegistrationProcessor.postRoutes(routes);
                            } catch (Exception e) {
                                e.printStackTrace();
                                fail();
                            }
                        }
                );
    }

    @TestConfiguration
    static class TestRegistrationConfiguration {
        @Bean
        TestController1 testController1() {
            return new TestController1();
        }
    }
}

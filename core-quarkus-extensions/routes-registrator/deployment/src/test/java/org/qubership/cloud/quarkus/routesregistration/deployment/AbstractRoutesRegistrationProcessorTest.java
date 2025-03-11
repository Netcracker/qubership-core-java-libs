package org.qubership.cloud.quarkus.routesregistration.deployment;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.qubership.cloud.quarkus.routesregistration.deployment.config.RequestRecorder;
import org.qubership.cloud.routesregistration.common.gateway.route.rest.CommonRequest;
import org.qubership.cloud.routesregistration.common.gateway.route.rest.CompositeRequest;
import org.qubership.cloud.routesregistration.common.gateway.route.v3.domain.RouteConfigurationRequestV3;
import jakarta.inject.Inject;
import okhttp3.Request;
import okio.Buffer;
import org.awaitility.Awaitility;

import org.awaitility.Durations;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.BeforeAll;

import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AbstractRoutesRegistrationProcessorTest {
    protected ObjectMapper om = new ObjectMapper();
    @Inject
    RequestRecorder requestRecorder;

    @ConfigProperty(name = "cloud.microservice.name")
    String microserviceName;

    @ConfigProperty(name = "cloud.microservice.namespace")
    String cloudNamespace;


    @BeforeAll
    public static void setUp() {
        Awaitility.setDefaultPollInterval(200, TimeUnit.MILLISECONDS);
        Awaitility.setDefaultPollDelay(Duration.ZERO);
        Awaitility.setDefaultTimeout(Durations.ONE_MINUTE);
    }

    protected Integer getSize() {
        return requestRecorder.records().size();
    }

    protected Set<RouteConfigurationRequestV3> retrieveReqV3Body() {
        return retrieveReqBody(
                requestRecorder.records(),
                request -> request.url().encodedPath().contains("/api/v3/routes"),
                new TypeReference<RouteConfigurationRequestV3>() {});
    }

    protected <T> Set<T> retrieveReqBody(Set<Request> requests, Predicate<Request> requestsFilter, TypeReference<T> typeReference) {
        return requests.stream()
                .filter(requestsFilter)
                .map(request -> {
                    Buffer buffer = new Buffer();
                    try {
                        assertNotNull(request.body());
                        request.body().writeTo(buffer);
                    } catch (IOException e) {
                        return null;
                    }
                    return buffer.readUtf8();
                })
                .filter(Objects::nonNull)
                .map(bodyJson -> {
                    try {
                        return om.readValue(bodyJson, typeReference);
                    } catch (JsonProcessingException e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    public static void assertRequestEquals(CompositeRequest<CommonRequest> expectedRequests, Set<RouteConfigurationRequestV3> actualRequests) {
        expectedRequests.forEach(expectedRequest -> assertTrue(containsRequest(actualRequests, expectedRequest.getPayload())));
        Collection<?> payloads = StreamSupport.stream(Spliterators.spliteratorUnknownSize(expectedRequests.iterator(), Spliterator.ORDERED), false)
                .map(CommonRequest::getPayload)
                .collect(Collectors.toList());
        actualRequests.forEach(actualRequest -> assertTrue(containsRequest(payloads, actualRequest)));
    }

    private static boolean containsRequest(Collection<?> requests, Object request) {
        for (Object actualRequest : requests) {
            if (actualRequest.equals(request)) {
                return true;
            }
        }
        return false;
    }
}

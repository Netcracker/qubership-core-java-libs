package com.netcracker.cloud.disableapi;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class DeprecatedApiExceptionTest {

    @Test
    void testDeprecatedApiException() {
        String requestMethod = "GET";
        String requestUri = "/api/v1/test";
        Set<String> matchingMethods = Stream.of("GET").collect(Collectors.toSet());
        String matchingUri = "/api/v1/**";
        DeprecatedApiException e = new DeprecatedApiException(requestMethod, requestUri, matchingMethods, matchingUri);
        Assertions.assertEquals("NC-COMMON-2101", e.getErrorCode().getCode());
        Assertions.assertEquals("Request is declined with 404 Not Found, because deprecated REST API is disabled", e.getErrorCode().getTitle());
        Assertions.assertEquals(String.format("Request [%s] '%s' is declined with 404 Not Found, because the following deprecated REST API is disabled: [%s] %s",
                requestMethod, requestUri, matchingMethods, matchingUri), e.getDetail());
    }
}

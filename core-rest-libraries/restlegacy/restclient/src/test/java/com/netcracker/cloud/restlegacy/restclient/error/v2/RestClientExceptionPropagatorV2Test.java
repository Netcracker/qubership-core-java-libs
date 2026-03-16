package com.netcracker.cloud.restlegacy.restclient.error.v2;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClientException;

import static org.junit.jupiter.api.Assertions.*;

class RestClientExceptionPropagatorV2Test {

    private final RestClientExceptionPropagatorV2 propagator = new RestClientExceptionPropagatorV2();
    private final String failedUrl = "http://test-service";

    @Test
    void propagate_shouldRethrowRestClientExceptionAsIs() {
        RestClientException ex = new RestClientException("Already RestClientException");

        RestClientException thrown = assertThrows(
                RestClientException.class,
                () -> propagator.propagate(ex, failedUrl)
        );

        assertSame(ex, thrown, "RestClientException should be propagated as-is");
    }

    @Test
    void propagate_shouldRethrowRuntimeExceptionAsIs() {
        RuntimeException ex = new RuntimeException("Runtime exception");

        RuntimeException thrown = assertThrows(
                RuntimeException.class,
                () -> propagator.propagate(ex, failedUrl)
        );

        assertSame(ex, thrown, "RuntimeException should be propagated as-is");
    }

    @Test
    void propagate_shouldRethrowErrorAsIs() {
        Error error = new OutOfMemoryError("Critical error");

        Error thrown = assertThrows(
                OutOfMemoryError.class,
                () -> propagator.propagate(error, failedUrl)
        );

        assertSame(error, thrown, "Error should be propagated as-is");
    }

    @Test
    void propagate_shouldWrapCheckedException() {
        Exception ex = new Exception("Checked exception");

        RestClientException thrown = assertThrows(
                RestClientException.class,
                () -> propagator.propagate(ex, failedUrl)
        );

        assertEquals("Exception while communication with service " + failedUrl, thrown.getMessage());
        assertSame(ex, thrown.getCause(), "Original exception should be wrapped as cause");
    }
}

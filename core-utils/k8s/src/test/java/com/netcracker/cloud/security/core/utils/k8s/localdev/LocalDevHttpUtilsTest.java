package com.netcracker.cloud.security.core.utils.k8s.localdev;

import org.junit.jupiter.api.Test;

import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LocalDevHttpUtilsTest {

    @Test
    void isUnauthorizedDetects401And403() {
        assertTrue(LocalDevHttpUtils.isUnauthorized(401));
        assertTrue(LocalDevHttpUtils.isUnauthorized(403));
        assertFalse(LocalDevHttpUtils.isUnauthorized(200));
    }

    @Test
    void isFailedDetectsNon2xx() {
        assertFalse(LocalDevHttpUtils.isFailed(200));
        assertFalse(LocalDevHttpUtils.isFailed(204));
        assertTrue(LocalDevHttpUtils.isFailed(400));
        assertTrue(LocalDevHttpUtils.isFailed(500));
    }

    @Test
    void ensureSuccessfulThrowsOnUnauthorizedAndFailed() {
        HttpResponse<String> unauthorized = mock(HttpResponse.class);
        when(unauthorized.statusCode()).thenReturn(401);
        when(unauthorized.body()).thenReturn("denied");

        IllegalStateException unauthorizedEx = assertThrows(IllegalStateException.class,
                () -> LocalDevHttpUtils.ensureSuccessful(unauthorized, "test operation"));
        assertTrue(unauthorizedEx.getMessage().contains("unauthorized"));
        assertTrue(unauthorizedEx.getMessage().contains("denied"));

        HttpResponse<String> failed = mock(HttpResponse.class);
        when(failed.statusCode()).thenReturn(500);
        when(failed.body()).thenReturn("boom");

        IllegalStateException failedEx = assertThrows(IllegalStateException.class,
                () -> LocalDevHttpUtils.ensureSuccessful(failed, "test operation"));
        assertTrue(failedEx.getMessage().contains("failed"));
        assertTrue(failedEx.getMessage().contains("boom"));
    }
}

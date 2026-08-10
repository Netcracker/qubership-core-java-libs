package com.netcracker.cloud.security.core.utils.k8s.localdev;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import java.net.http.HttpResponse;

import static com.netcracker.cloud.security.core.utils.k8s.localdev.LocalDevConstants.MAX_ERROR_BODY_LENGTH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LocalDevUtilsTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void getTextFieldHandlesMissingBlankAndNonText() {
        ObjectNode node = MAPPER.createObjectNode();
        assertNull(LocalDevUtils.getTextField(node, "missing"));
        node.putNull("nullField");
        assertNull(LocalDevUtils.getTextField(node, "nullField"));
        node.put("blank", "  ");
        assertNull(LocalDevUtils.getTextField(node, "blank"));
        node.put("number", 42);
        assertEquals("42", LocalDevUtils.getTextField(node, "number"));
        node.put("value", " text ");
        assertEquals(" text ", LocalDevUtils.getTextField(node, "value"));
    }

    @Test
    void firstNonBlankReturnsFirstNonBlankValue() {
        assertEquals("first", LocalDevUtils.firstNonBlank("first", "second"));
        assertEquals("second", LocalDevUtils.firstNonBlank(null, "second"));
        assertNull(LocalDevUtils.firstNonBlank(null, null));
    }

    @Test
    void truncateResponseBodyHandlesNullAndLongBodies() {
        assertEquals("", LocalDevUtils.truncateResponseBody(null));
        assertEquals("short", LocalDevUtils.truncateResponseBody("short"));
        String longBody = "x".repeat(MAX_ERROR_BODY_LENGTH + 10);
        String truncated = LocalDevUtils.truncateResponseBody(longBody);
        assertEquals(MAX_ERROR_BODY_LENGTH + 3, truncated.length());
        assertEquals("...", truncated.substring(truncated.length() - 3));
    }

    @Test
    void padBase64UrlPadsToMultipleOfFour() {
        assertEquals("abcd", LocalDevUtils.padBase64Url("abcd"));
        assertEquals("abc=", LocalDevUtils.padBase64Url("abc"));
    }

    @Test
    void isFailedDetectsNon2xx() {
        assertFalse(LocalDevUtils.isFailed(200));
        assertFalse(LocalDevUtils.isFailed(204));
        assertTrue(LocalDevUtils.isFailed(401));
        assertTrue(LocalDevUtils.isFailed(403));
        assertTrue(LocalDevUtils.isFailed(400));
        assertTrue(LocalDevUtils.isFailed(500));
    }

    @Test
    void ensureSuccessfulThrowsOnFailed() {
        HttpResponse<String> unauthorized = mock(HttpResponse.class);
        when(unauthorized.statusCode()).thenReturn(401);
        when(unauthorized.body()).thenReturn("denied");

        IllegalStateException unauthorizedEx = assertThrows(IllegalStateException.class,
                () -> LocalDevUtils.ensureSuccessful(unauthorized, "test operation"));
        assertTrue(unauthorizedEx.getMessage().contains("failed"));
        assertTrue(unauthorizedEx.getMessage().contains("denied"));

        HttpResponse<String> failed = mock(HttpResponse.class);
        when(failed.statusCode()).thenReturn(500);
        when(failed.body()).thenReturn("boom");

        IllegalStateException failedEx = assertThrows(IllegalStateException.class,
                () -> LocalDevUtils.ensureSuccessful(failed, "test operation"));
        assertTrue(failedEx.getMessage().contains("failed"));
        assertTrue(failedEx.getMessage().contains("boom"));
    }
}

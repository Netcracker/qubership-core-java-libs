package com.netcracker.cloud.security.core.utils.k8s.localdev;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import static com.netcracker.cloud.security.core.utils.k8s.localdev.LocalDevConstants.MAX_ERROR_BODY_LENGTH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class LocalDevJsonUtilsTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void getTextFieldHandlesMissingBlankAndNonText() {
        ObjectNode node = MAPPER.createObjectNode();
        assertNull(LocalDevJsonUtils.getTextField(node, "missing"));
        node.putNull("nullField");
        assertNull(LocalDevJsonUtils.getTextField(node, "nullField"));
        node.put("blank", "  ");
        assertNull(LocalDevJsonUtils.getTextField(node, "blank"));
        node.put("number", 42);
        assertEquals("42", LocalDevJsonUtils.getTextField(node, "number"));
        node.put("value", " text ");
        assertEquals(" text ", LocalDevJsonUtils.getTextField(node, "value"));
    }

    @Test
    void firstNonBlankReturnsFirstNonBlankValue() {
        assertEquals("first", LocalDevJsonUtils.firstNonBlank("first", "second"));
        assertEquals("second", LocalDevJsonUtils.firstNonBlank(null, "second"));
        assertNull(LocalDevJsonUtils.firstNonBlank(null, null));
    }

    @Test
    void truncateResponseBodyHandlesNullAndLongBodies() {
        assertEquals("", LocalDevJsonUtils.truncateResponseBody(null));
        assertEquals("short", LocalDevJsonUtils.truncateResponseBody("short"));
        String longBody = "x".repeat(MAX_ERROR_BODY_LENGTH + 10);
        String truncated = LocalDevJsonUtils.truncateResponseBody(longBody);
        assertEquals(MAX_ERROR_BODY_LENGTH + 3, truncated.length());
        assertEquals("...", truncated.substring(truncated.length() - 3));
    }

    @Test
    void padBase64UrlPadsToMultipleOfFour() {
        assertEquals("abcd", LocalDevJsonUtils.padBase64Url("abcd"));
        assertEquals("abc=", LocalDevJsonUtils.padBase64Url("abc"));
    }
}

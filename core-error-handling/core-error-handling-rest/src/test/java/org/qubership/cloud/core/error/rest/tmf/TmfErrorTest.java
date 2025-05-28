package org.qubership.cloud.core.error.rest.tmf;

import org.junit.jupiter.api.Test;
import org.qubership.cloud.core.error.runtime.ErrorCode;
import org.qubership.cloud.core.error.runtime.ErrorCodeException;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TmfErrorTest {

    @Test
    void testDefaultConstructor() {
        TmfError error = new TmfError();

        assertNotNull(error);
        assertNull(error.getId());
        assertNull(error.getReferenceError());
        assertNull(error.getCode());
        assertNull(error.getReason());
        assertNull(error.getDetail());
        assertNull(error.getStatus());
        assertNull(error.getSource());
        assertNotNull(error.getMeta());
        assertTrue(error.getMeta().isEmpty());
    }

    @Test
    void testAllArgsConstructor() {
        TestData testData = createTestData();
        TmfError error = new TmfError(
            testData.id,
            testData.referenceError,
            testData.code,
            testData.reason,
            testData.detail,
            testData.status,
            testData.source,
            testData.meta
        );

        assertTmfErrorFields(error, testData);
    }

    @Test
    void testBuilderPattern() {
        TestData testData = createTestData();
        TmfError error = TmfError.builder()
                .id(testData.id)
                .referenceError(testData.referenceError)
                .code(testData.code)
                .reason(testData.reason)
                .detail(testData.detail)
                .status(testData.status)
                .source(testData.source)
                .meta(testData.meta)
                .build();

        assertTmfErrorFields(error, testData);
    }

    @Test
    void testBuilderWithErrorCodeException() {
        ErrorCode mockErrorCode = mock(ErrorCode.class);
        when(mockErrorCode.getCode()).thenReturn("test-code");
        when(mockErrorCode.getTitle()).thenReturn("test-title");

        ErrorCodeException mockException = mock(ErrorCodeException.class);
        when(mockException.getId()).thenReturn("test-id");
        when(mockException.getErrorCode()).thenReturn(mockErrorCode);
        when(mockException.getDetail()).thenReturn("test-detail");

        TmfError error = TmfError.builder(mockException).build();

        assertEquals("test-id", error.getId());
        assertEquals("test-code", error.getCode());
        assertEquals("test-title", error.getReason());
        assertEquals("test-detail", error.getDetail());
    }

    @Test
    void testBuilderWithNullErrorCodeException() {
        assertThrows(IllegalArgumentException.class, () -> TmfError.builder(null));
    }

    @Test
    void testToString() {
        TmfError error = TmfError.builder()
                .id("test-id")
                .code("test-code")
                .build();

        String toString = error.toString();
        assertTrue(toString.contains("test-id"));
        assertTrue(toString.contains("test-code"));
    }

    // Helper class to hold test data
    private static class TestData {
        final String id;
        final String referenceError;
        final String code;
        final String reason;
        final String detail;
        final String status;
        final Object source;
        final Map<String, Object> meta;

        TestData(String id, String referenceError, String code, String reason, 
                String detail, String status, Object source, Map<String, Object> meta) {
            this.id = id;
            this.referenceError = referenceError;
            this.code = code;
            this.reason = reason;
            this.detail = detail;
            this.status = status;
            this.source = source;
            this.meta = meta;
        }
    }

    // Helper methods
    private TestData createTestData() {
        String id = "test-id";
        String referenceError = "ref-error";
        String code = "test-code";
        String reason = "test-reason";
        String detail = "test-detail";
        String status = "test-status";
        Object source = new Object();
        Map<String, Object> meta = new HashMap<>();
        meta.put("key", "value");

        return new TestData(id, referenceError, code, reason, detail, status, source, meta);
    }

    private void assertTmfErrorFields(TmfError error, TestData testData) {
        assertEquals(testData.id, error.getId());
        assertEquals(testData.referenceError, error.getReferenceError());
        assertEquals(testData.code, error.getCode());
        assertEquals(testData.reason, error.getReason());
        assertEquals(testData.detail, error.getDetail());
        assertEquals(testData.status, error.getStatus());
        assertEquals(testData.source, error.getSource());
        assertEquals(testData.meta, error.getMeta());
    }
}

package org.qubership.cloud.core.error.rest.tmf;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.qubership.cloud.core.error.rest.exception.RemoteCodeException;
import org.qubership.cloud.core.error.rest.exception.RemoteMultiCauseException;
import org.qubership.cloud.core.error.runtime.ErrorCodeException;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DefaultTmfErrorResponseConverterTest {

    private DefaultTmfErrorResponseConverter converter;

    @BeforeEach
    void setUp() {
        converter = new DefaultTmfErrorResponseConverter();
    }

    @Test
    void buildErrorCodeException_WithSingleError_ShouldCreateRemoteCodeException() {
        // Arrange
        TmfErrorResponse response = createTmfErrorResponse("TEST_CODE", "Test Reason", "Test Detail", "test-id", "400", "test-source");
        Map<String, Object> meta = new HashMap<>();
        meta.put("key", "value");
        response.setMeta(meta);
        response.setErrors(Collections.emptyList());

        // Act
        RemoteCodeException exception = converter.buildErrorCodeException(response);

        // Assert
        assertRemoteCodeException(exception, "TEST_CODE", "Test Reason", "[TEST_CODE][test-id] Test Detail", "test-id", 400, "test-source", "value");
    }

    @Test
    void buildErrorCodeException_WithMultipleErrors_ShouldCreateRemoteMultiCauseException() {
        // Arrange
        TmfErrorResponse response = createTmfErrorResponse("PARENT_CODE", "Parent Reason", "Parent Detail", "parent-id", null, null);
        TmfError childError = createTmfError("CHILD_CODE", "Child Reason", "child-id", null, null);
        response.setErrors(List.of(childError));

        // Act
        RemoteCodeException exception = converter.buildErrorCodeException(response);

        // Assert
        assertInstanceOf(RemoteMultiCauseException.class, exception);
        assertRemoteCodeException(exception, "PARENT_CODE", "Parent Reason", "[PARENT_CODE][parent-id] Parent Detail", "parent-id", null, null, null);
        
        RemoteMultiCauseException multiCauseException = (RemoteMultiCauseException) exception;
        assertEquals(1, multiCauseException.getCauseExceptions().size());
        
        ErrorCodeException cause = multiCauseException.getCauseExceptions().get(0);
        assertInstanceOf(RemoteCodeException.class, cause);
        RemoteCodeException remoteCause = (RemoteCodeException) cause;
        assertRemoteCodeException(remoteCause, "CHILD_CODE", "Child Reason", "[CHILD_CODE][child-id] Child Reason", "child-id", null, null, null);
    }

    @Test
    void buildException_ShouldCreateRemoteCodeException() {
        // Arrange
        TmfError error = createTmfError("TEST_CODE", "Test Reason", "test-id", "500", "test-source");
        Map<String, Object> meta = new HashMap<>();
        meta.put("key", "value");
        error.setMeta(meta);

        // Act
        RemoteCodeException exception = converter.buildException(error);

        // Assert
        assertRemoteCodeException(exception, "TEST_CODE", "Test Reason", "[TEST_CODE][test-id] Test Reason", "test-id", 500, "test-source", "value");
    }

    @Test
    void buildErrorCodeException_WithNullFields_ShouldHandleGracefully() {
        // Arrange
        TmfErrorResponse response = createTmfErrorResponse("TEST_CODE", "Test Reason", null, "test-id", null, null);

        // Act
        RemoteCodeException exception = converter.buildErrorCodeException(response);

        // Assert
        assertRemoteCodeException(exception, "TEST_CODE", "Test Reason", "[TEST_CODE][test-id] Test Reason", "test-id", null, null, null);
    }

    @Test
    void buildException_WithNullFields_ShouldHandleGracefully() {
        // Arrange
        TmfError error = createTmfError("TEST_CODE", "Test Reason", "test-id", null, null);

        // Act
        RemoteCodeException exception = converter.buildException(error);

        // Assert
        assertRemoteCodeException(exception, "TEST_CODE", "Test Reason", "[TEST_CODE][test-id] Test Reason", "test-id", null, null, null);
    }

    private TmfErrorResponse createTmfErrorResponse(String code, String reason, String detail, String id, String status, String source) {
        TmfErrorResponse response = new TmfErrorResponse();
        response.setCode(code);
        response.setReason(reason);
        response.setDetail(detail);
        response.setId(id);
        response.setStatus(status);
        response.setSource(source);
        return response;
    }

    private TmfError createTmfError(String code, String reason, String id, String status, String source) {
        TmfError error = new TmfError();
        error.setCode(code);
        error.setReason(reason);
        error.setId(id);
        error.setStatus(status);
        error.setSource(source);
        return error;
    }

    private void assertRemoteCodeException(RemoteCodeException exception, String code, String reason, String message, 
                                         String id, Integer status, String source, String metaValue) {
        assertNotNull(exception);
        assertEquals(code, exception.getErrorCode().getCode());
        assertEquals(reason, exception.getErrorCode().getTitle());
        assertEquals(message, exception.getMessage());
        assertEquals(id, exception.getId());
        assertEquals(status, exception.getStatus());
        assertEquals(source, exception.getSource());
        if (metaValue != null) {
            assertEquals(metaValue, exception.getMeta().get("key"));
        } else {
            assertTrue(exception.getMeta().isEmpty());
        }
        assertEquals(0, exception.getStackTrace().length);
    }
}

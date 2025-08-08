package org.qubership.cloud.core.error.rest.tmf;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.qubership.cloud.core.error.runtime.ErrorCode;
import org.qubership.cloud.core.error.runtime.ErrorCodeException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TmfErrorResponseTest {

    private static final String TEST_ID = "test-id";
    private static final String TEST_REFERENCE_ERROR = "ref-error";
    private static final String TEST_CODE = "test-code";
    private static final String TEST_REASON = "test-reason";
    private static final String TEST_DETAIL = "test-detail";
    private static final String TEST_STATUS = "test-status";
    private static final String TEST_TYPE = "test-type";
    private static final String TEST_SCHEMA_LOCATION = "test-schema";

    @Mock
    private ErrorCode mockErrorCode;

    @Test
    void testDefaultConstructor() {
        TmfErrorResponse response = new TmfErrorResponse();
        assertNotNull(response);
        assertNotNull(response.getMeta());
        assertNotNull(response.getErrors());
        assertTrue(response.getMeta().isEmpty());
        assertTrue(response.getErrors().isEmpty());
    }

    @Test
    void testAllArgsConstructor() {
        Object source = new Object();
        Map<String, Object> meta = new HashMap<>();
        List<TmfError> errors = List.of(new TmfError());

        TmfErrorResponse response = new TmfErrorResponse(
            TEST_ID, TEST_REFERENCE_ERROR, TEST_CODE, TEST_REASON, TEST_DETAIL, TEST_STATUS,
            source, meta, errors, TEST_TYPE, TEST_SCHEMA_LOCATION
        );

        assertResponseFields(response, source, meta, errors);
    }

    @Test
    void testBuilder() {
        Object source = new Object();
        Map<String, Object> meta = new HashMap<>();
        List<TmfError> errors = List.of(new TmfError());

        TmfErrorResponse response = TmfErrorResponse.builder()
            .id(TEST_ID)
            .referenceError(TEST_REFERENCE_ERROR)
            .code(TEST_CODE)
            .reason(TEST_REASON)
            .detail(TEST_DETAIL)
            .status(TEST_STATUS)
            .source(source)
            .meta(meta)
            .errors(errors)
            .type(TEST_TYPE)
            .schemaLocation(TEST_SCHEMA_LOCATION)
            .build();

        assertResponseFields(response, source, meta, errors);
    }

    @Test
    void testBuilderWithErrorCodeException() {
        when(mockErrorCode.getCode()).thenReturn(TEST_CODE);
        when(mockErrorCode.getTitle()).thenReturn(TEST_REASON);

        ErrorCodeException exception = new ErrorCodeException(mockErrorCode, TEST_DETAIL);
        exception.setId(TEST_ID);

        TmfErrorResponse response = TmfErrorResponse.builder(exception).build();

        assertNotNull(response);
        assertEquals(TEST_ID, response.getId());
        assertEquals(TEST_CODE, response.getCode());
        assertEquals(TEST_REASON, response.getReason());
        assertEquals(TEST_DETAIL, response.getDetail());
        assertEquals(TmfErrorResponse.TYPE_V1_0, response.getType());
    }

    @Test
    void testBuilderWithErrorCodeException_NullException() {
        assertThrows(IllegalArgumentException.class, () -> TmfErrorResponse.builder(null));
    }

    @Test
    void testGettersAndSetters() {
        TmfErrorResponse response = new TmfErrorResponse();
        Object source = new Object();
        Map<String, Object> meta = new HashMap<>();
        List<TmfError> errors = List.of(new TmfError());
        
        response.setId(TEST_ID);
        response.setReferenceError(TEST_REFERENCE_ERROR);
        response.setCode(TEST_CODE);
        response.setReason(TEST_REASON);
        response.setDetail(TEST_DETAIL);
        response.setStatus(TEST_STATUS);
        response.setSource(source);
        response.setMeta(meta);
        response.setErrors(errors);
        response.setType(TEST_TYPE);
        response.setSchemaLocation(TEST_SCHEMA_LOCATION);

        assertResponseFields(response, source, meta, errors);
    }

    private void assertResponseFields(TmfErrorResponse response, Object source, Map<String, Object> meta, List<TmfError> errors) {
        assertNotNull(response);
        assertEquals(TEST_ID, response.getId());
        assertEquals(TEST_REFERENCE_ERROR, response.getReferenceError());
        assertEquals(TEST_CODE, response.getCode());
        assertEquals(TEST_REASON, response.getReason());
        assertEquals(TEST_DETAIL, response.getDetail());
        assertEquals(TEST_STATUS, response.getStatus());
        assertEquals(source, response.getSource());
        assertEquals(meta, response.getMeta());
        assertEquals(errors, response.getErrors());
        assertEquals(TEST_TYPE, response.getType());
        assertEquals(TEST_SCHEMA_LOCATION, response.getSchemaLocation());
    }
} 
package org.qubership.cloud.core.error.runtime;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MultiCauseExceptionTest {

    private static final ErrorCode CUSTOM_ERROR_CODE = new ErrorCodeHolder("TEST-CODE", "test message");
    private static final String CUSTOM_MESSAGE = "custom error message";

    private void assertMessageFormat(MultiCauseException exception, ErrorCode expectedErrorCode, String expectedMessage) {
        assertTrue(exception.getMessage().startsWith("[" + expectedErrorCode.getCode() + "]["));
        assertTrue(exception.getMessage().endsWith(expectedMessage));
    }

    @Test
    void constructorWithCauses_ShouldCreateExceptionWithDefaultErrorCode() {
        // Given
        List<ErrorCodeException> causes = Arrays.asList(
            new ErrorCodeException(CUSTOM_ERROR_CODE, "cause1"),
            new ErrorCodeException(CUSTOM_ERROR_CODE, "cause2")
        );

        // When
        MultiCauseException exception = new MultiCauseException(causes);

        // Then
        assertEquals(MultiCauseException.DEFAULT_ERROR_CODE, exception.getErrorCode());
        assertMessageFormat(exception, MultiCauseException.DEFAULT_ERROR_CODE, "multiple independent errors have happened");
        assertEquals(2, exception.getSuppressed().length);
        assertEquals(2, exception.getCauseExceptions().size());
    }

    @Test
    void constructorWithCauses_ShouldHandleEmptyCollection() {
        // Given
        List<ErrorCodeException> emptyCauses = Collections.emptyList();

        // When
        MultiCauseException exception = new MultiCauseException(emptyCauses);

        // Then
        assertEquals(MultiCauseException.DEFAULT_ERROR_CODE, exception.getErrorCode());
        assertMessageFormat(exception, MultiCauseException.DEFAULT_ERROR_CODE, "multiple independent errors have happened");
        assertEquals(0, exception.getSuppressed().length);
        assertTrue(exception.getCauseExceptions().isEmpty());
    }

    @Test
    void constructorWithErrorCodeAndMessage_ShouldCreateExceptionWithCustomValues() {
        // Given
        List<ErrorCodeException> causes = Arrays.asList(
            new ErrorCodeException(CUSTOM_ERROR_CODE, "cause1"),
            new ErrorCodeException(CUSTOM_ERROR_CODE, "cause2")
        );

        // When
        MultiCauseException exception = new MultiCauseException(CUSTOM_ERROR_CODE, CUSTOM_MESSAGE, causes);

        // Then
        assertEquals(CUSTOM_ERROR_CODE, exception.getErrorCode());
        assertMessageFormat(exception, CUSTOM_ERROR_CODE, CUSTOM_MESSAGE);
        assertEquals(2, exception.getSuppressed().length);
        assertEquals(2, exception.getCauseExceptions().size());
    }

    @Test
    void constructorWithErrorCodeAndMessage_ShouldHandleEmptyCollection() {
        // Given
        List<ErrorCodeException> emptyCauses = Collections.emptyList();

        // When
        MultiCauseException exception = new MultiCauseException(CUSTOM_ERROR_CODE, CUSTOM_MESSAGE, emptyCauses);

        // Then
        assertEquals(CUSTOM_ERROR_CODE, exception.getErrorCode());
        assertMessageFormat(exception, CUSTOM_ERROR_CODE, CUSTOM_MESSAGE);
        assertEquals(0, exception.getSuppressed().length);
        assertTrue(exception.getCauseExceptions().isEmpty());
    }

    @Test
    void getCauseExceptions_ShouldFilterNonErrorCodeExceptions() {
        // Given
        ErrorCodeException errorCodeException = new ErrorCodeException(CUSTOM_ERROR_CODE, "error code exception");
        RuntimeException runtimeException = new RuntimeException("runtime exception");
        
        // Create exception with mixed causes
        MultiCauseException exception = new MultiCauseException(Collections.singletonList(errorCodeException));
        exception.addSuppressed(runtimeException);

        // When
        List<ErrorCodeException> causeExceptions = exception.getCauseExceptions();

        // Then
        assertEquals(1, causeExceptions.size());
        assertEquals(errorCodeException, causeExceptions.get(0));
        assertEquals(2, exception.getSuppressed().length);
    }

    @Test
    void getCauseExceptions_ShouldReturnEmptyList_WhenNoSuppressedExceptions() {
        // Given
        MultiCauseException exception = new MultiCauseException(Collections.emptyList());

        // When
        List<ErrorCodeException> causeExceptions = exception.getCauseExceptions();

        // Then
        assertTrue(causeExceptions.isEmpty());
    }
}

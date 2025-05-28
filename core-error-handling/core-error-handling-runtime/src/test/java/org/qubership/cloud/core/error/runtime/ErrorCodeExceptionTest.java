package org.qubership.cloud.core.error.runtime;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ErrorCodeExceptionTest {

    private static final String TEST_CODE = "TEST-0001";
    private static final String TEST_TITLE = "Test Error";
    private static final String TEST_DETAIL = "Test error detail";
    private static final String TEST_MESSAGE = "Test message";

    @Test
    void testConstructorWithErrorCodeAndDetail() {
        ErrorCode errorCode = createMockErrorCode();
        ErrorCodeException exception = new ErrorCodeException(errorCode, TEST_DETAIL);

        verifyBasicProperties(exception, errorCode, TEST_DETAIL, null);
        verifyMessageFormat(exception, TEST_DETAIL);
    }

    @Test
    void testConstructorWithErrorCodeDetailAndCause() {
        ErrorCode errorCode = createMockErrorCode();
        Throwable cause = new RuntimeException(TEST_MESSAGE);
        ErrorCodeException exception = new ErrorCodeException(errorCode, TEST_DETAIL, cause);

        verifyBasicProperties(exception, errorCode, TEST_DETAIL, cause);
        verifyMessageFormat(exception, TEST_DETAIL);
    }

    @Test
    void testConstructorWithAllParameters() {
        ErrorCode errorCode = createMockErrorCode();
        Throwable cause = new RuntimeException(TEST_MESSAGE);
        ErrorCodeException exception = new ErrorCodeException(errorCode, TEST_DETAIL, cause, true, true);

        verifyBasicProperties(exception, errorCode, TEST_DETAIL, cause);
        verifyMessageFormat(exception, TEST_DETAIL);
    }

    @Test
    void testConstructorWithAllParametersAndDisabledStackTrace() {
        ErrorCode errorCode = createMockErrorCode();
        Throwable cause = new RuntimeException(TEST_MESSAGE);
        ErrorCodeException exception = new ErrorCodeException(errorCode, TEST_DETAIL, cause, false, false);

        verifyBasicProperties(exception, errorCode, TEST_DETAIL, cause);
        verifyMessageFormat(exception, TEST_DETAIL);
    }

    @Test
    void testGetMessageWithNullDetail() {
        ErrorCode errorCode = createMockErrorCode();
        ErrorCodeException exception = new ErrorCodeException(errorCode, null);

        verifyBasicProperties(exception, errorCode, null, null);
        verifyMessageFormat(exception, TEST_TITLE);
    }

    @Test
    void testConstructorWithNullErrorCode() {
        assertThrows(IllegalArgumentException.class, () ->
            new ErrorCodeException(null, TEST_DETAIL));
    }

    @Test
    void testIdSetter() {
        ErrorCode errorCode = createMockErrorCode();
        ErrorCodeException exception = new ErrorCodeException(errorCode, TEST_DETAIL);
        String newId = "new-id";

        exception.setId(newId);
        assertEquals(newId, exception.getId());
    }

    private ErrorCode createMockErrorCode() {
        ErrorCode mockErrorCode = mock(ErrorCode.class);
        when(mockErrorCode.getCode()).thenReturn(TEST_CODE);
        when(mockErrorCode.getTitle()).thenReturn(TEST_TITLE);
        return mockErrorCode;
    }

    private void verifyBasicProperties(ErrorCodeException exception, ErrorCode expectedErrorCode,
                                     String expectedDetail, Throwable expectedCause) {
        assertNotNull(exception.getId());
        assertEquals(expectedErrorCode, exception.getErrorCode());
        assertEquals(expectedDetail, exception.getDetail());
        assertEquals(expectedCause, exception.getCause());
    }

    private void verifyMessageFormat(ErrorCodeException exception, String expectedMessage) {
        assertEquals(String.format("[%s][%s] %s", TEST_CODE, exception.getId(), expectedMessage),
                    exception.getMessage());
    }
}

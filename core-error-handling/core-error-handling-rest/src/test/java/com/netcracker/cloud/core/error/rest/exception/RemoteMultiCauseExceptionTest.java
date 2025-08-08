package org.qubership.cloud.core.error.rest.exception;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.qubership.cloud.core.error.runtime.ErrorCode;
import org.qubership.cloud.core.error.runtime.ErrorCodeException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RemoteMultiCauseExceptionTest {

    @Mock
    private ErrorCode errorCode;

    @Mock
    private RemoteCodeException remoteCodeException1;

    @Mock
    private RemoteCodeException remoteCodeException2;

    @Test
    void constructorShouldAddSuppressedExceptions() {
        // Given
        String message = "Test message";
        List<RemoteCodeException> causes = List.of(remoteCodeException1, remoteCodeException2);

        // When
        RemoteMultiCauseException exception = new RemoteMultiCauseException(errorCode, message, causes);

        // Then
        assertEquals(errorCode, exception.getErrorCode());
        assertTrue(exception.getMessage().contains(message));
        assertEquals(2, exception.getSuppressed().length);
        assertArrayEquals(causes.toArray(), exception.getSuppressed());
    }

    @Test
    void getCauseExceptionsShouldReturnAllRemoteCodeExceptions() {
        // Given
        List<RemoteCodeException> causes = List.of(remoteCodeException1, remoteCodeException2);
        RemoteMultiCauseException exception = createException(causes);

        // When
        List<ErrorCodeException> result = exception.getCauseExceptions();

        // Then
        assertExceptionListContainsAll(result, causes);
    }

    @Test
    void getCauseExceptionsShouldReturnAllExceptions() {
        // Given
        TestRemoteCodeException testException = createTestException("test-error-id");
        List<RemoteCodeException> causes = List.of(remoteCodeException1, testException);
        RemoteMultiCauseException exception = createException(causes);

        // When
        List<ErrorCodeException> result = exception.getCauseExceptions();

        // Then
        assertExceptionListContainsAll(result, causes);
    }

    @Test
    void getCauseErrorIdsShouldReturnIdsFromAllExceptions() {
        // Given
        String errorId1 = "test-error-id-1";
        String errorId2 = "test-error-id-2";
        when(remoteCodeException1.getId()).thenReturn(errorId1);
        TestRemoteCodeException testException = createTestException(errorId2);
        List<RemoteCodeException> causes = List.of(remoteCodeException1, testException);
        RemoteMultiCauseException exception = createException(causes);

        // When
        List<String> result = exception.getCauseErrorIds();

        // Then
        assertErrorIdsContainsAll(result, List.of(errorId1, errorId2));
    }

    @Test
    void getCauseErrorIdsShouldReturnIdsFromAllRemoteCodeExceptions() {
        // Given
        String errorId1 = "test-error-id-1";
        String errorId2 = "test-error-id-2";
        when(remoteCodeException1.getId()).thenReturn(errorId1);
        when(remoteCodeException2.getId()).thenReturn(errorId2);
        List<RemoteCodeException> causes = List.of(remoteCodeException1, remoteCodeException2);
        RemoteMultiCauseException exception = createException(causes);

        // When
        List<String> result = exception.getCauseErrorIds();

        // Then
        assertErrorIdsContainsAll(result, List.of(errorId1, errorId2));
    }

    // ------------------------------------------------------------------------------------

    private static class TestRemoteCodeException extends RemoteCodeException {
        private final String id;

        public TestRemoteCodeException(ErrorCode errorCode, String message, String id) {
            super(errorCode, message);
            this.id = id;
        }

        @Override
        public String getId() {
            return id;
        }
    }

    private RemoteMultiCauseException createException(List<RemoteCodeException> causes) {
        return new RemoteMultiCauseException(errorCode, "Test message", causes);
    }

    private TestRemoteCodeException createTestException(String id) {
        return new TestRemoteCodeException(errorCode, "Test message", id);
    }

    private void assertExceptionListContainsAll(List<ErrorCodeException> result, List<RemoteCodeException> expected) {
        assertEquals(expected.size(), result.size());
        expected.forEach(exception -> assertTrue(result.contains(exception)));
    }

    private void assertErrorIdsContainsAll(List<String> result, List<String> expected) {
        assertEquals(expected.size(), result.size());
        expected.forEach(id -> assertTrue(result.contains(id)));
    }
}

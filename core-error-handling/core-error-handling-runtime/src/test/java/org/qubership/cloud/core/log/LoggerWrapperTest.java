package org.qubership.cloud.core.log;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.qubership.cloud.utils.TestLowerException;
import org.qubership.cloud.utils.TestUpperException;
import org.slf4j.Logger;
import org.slf4j.Marker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoggerWrapperTest {

    @Mock
    private Logger mockLogger;

    @Mock
    private Marker mockMarker;

    private LoggerWrapper loggerWrapper;
    private static final String TEST_LOGGER_NAME = "test.logger";

    @BeforeEach
    void setUp() {
        loggerWrapper = new LoggerWrapper(TEST_LOGGER_NAME) {
            @Override
            protected Logger createLogger(String name) {
                return mockLogger;
            }
        };
    }

    @Nested
    class EnabledChecks {
        @Test
        void testGetName() {
            when(mockLogger.getName()).thenReturn(TEST_LOGGER_NAME);
            assertThat(loggerWrapper.getName()).isEqualTo(TEST_LOGGER_NAME);
            verify(mockLogger).getName();
        }

        @Test
        void testIsTraceEnabled() {
            when(mockLogger.isTraceEnabled()).thenReturn(true);
            assertThat(loggerWrapper.isTraceEnabled()).isTrue();
            verify(mockLogger).isTraceEnabled();
        }

        @Test
        void testIsTraceEnabledWithMarker() {
            when(mockLogger.isTraceEnabled(mockMarker)).thenReturn(true);
            assertThat(loggerWrapper.isTraceEnabled(mockMarker)).isTrue();
            verify(mockLogger).isTraceEnabled(mockMarker);
        }

        @Test
        void testIsDebugEnabled() {
            when(mockLogger.isDebugEnabled()).thenReturn(true);
            assertThat(loggerWrapper.isDebugEnabled()).isTrue();
            verify(mockLogger).isDebugEnabled();
        }

        @Test
        void testIsDebugEnabledWithMarker() {
            when(mockLogger.isDebugEnabled(mockMarker)).thenReturn(true);
            assertThat(loggerWrapper.isDebugEnabled(mockMarker)).isTrue();
            verify(mockLogger).isDebugEnabled(mockMarker);
        }

        @Test
        void testIsInfoEnabled() {
            when(mockLogger.isInfoEnabled()).thenReturn(true);
            assertThat(loggerWrapper.isInfoEnabled()).isTrue();
            verify(mockLogger).isInfoEnabled();
        }

        @Test
        void testIsInfoEnabledWithMarker() {
            when(mockLogger.isInfoEnabled(mockMarker)).thenReturn(true);
            assertThat(loggerWrapper.isInfoEnabled(mockMarker)).isTrue();
            verify(mockLogger).isInfoEnabled(mockMarker);
        }

        @Test
        void testIsWarnEnabled() {
            when(mockLogger.isWarnEnabled()).thenReturn(true);
            assertThat(loggerWrapper.isWarnEnabled()).isTrue();
            verify(mockLogger).isWarnEnabled();
        }

        @Test
        void testIsWarnEnabledWithMarker() {
            when(mockLogger.isWarnEnabled(mockMarker)).thenReturn(true);
            assertThat(loggerWrapper.isWarnEnabled(mockMarker)).isTrue();
            verify(mockLogger).isWarnEnabled(mockMarker);
        }

        @Test
        void testIsErrorEnabled() {
            when(mockLogger.isErrorEnabled()).thenReturn(true);
            assertThat(loggerWrapper.isErrorEnabled()).isTrue();
            verify(mockLogger).isErrorEnabled();
        }

        @Test
        void testIsErrorEnabledWithMarker() {
            when(mockLogger.isErrorEnabled(mockMarker)).thenReturn(true);
            assertThat(loggerWrapper.isErrorEnabled(mockMarker)).isTrue();
            verify(mockLogger).isErrorEnabled(mockMarker);
        }
    }

    @Nested
    class BasicLogging {
        private final Object arg = "arg";
        private final Object arg2 = "arg2";
        private final Throwable throwable = new RuntimeException("test exception");

        @Test
        void testTraceMethods() {
            String message = "test message";

            loggerWrapper.trace(message);
            loggerWrapper.trace(message, arg);
            loggerWrapper.trace(message, arg, arg2);
            loggerWrapper.trace(message, new Object[]{arg, arg2});
            loggerWrapper.trace(message, throwable);

            verify(mockLogger).trace(message);
            verify(mockLogger).trace(message, arg);
            verify(mockLogger).trace(message, arg, arg2);
            verify(mockLogger).trace(message, new Object[]{arg, arg2});
            verify(mockLogger).trace(message, throwable);
        }

        @Test
        void testDebugMethods() {
            String message = "test message";

            loggerWrapper.debug(message);
            loggerWrapper.debug(message, arg);
            loggerWrapper.debug(message, arg, arg2);
            loggerWrapper.debug(message, new Object[]{arg, arg2});
            loggerWrapper.debug(message, throwable);

            verify(mockLogger).debug(message);
            verify(mockLogger).debug(message, arg);
            verify(mockLogger).debug(message, arg, arg2);
            verify(mockLogger).debug(message, new Object[]{arg, arg2});
            verify(mockLogger).debug(message, throwable);
        }

        @Test
        void testInfoMethods() {
            String message = "test message";

            loggerWrapper.info(message);
            loggerWrapper.info(message, arg);
            loggerWrapper.info(message, arg, arg2);
            loggerWrapper.info(message, new Object[]{arg, arg2});
            loggerWrapper.info(message, throwable);

            verify(mockLogger).info(message);
            verify(mockLogger).info(message, arg);
            verify(mockLogger).info(message, arg, arg2);
            verify(mockLogger).info(message, new Object[]{arg, arg2});
            verify(mockLogger).info(message, throwable);
        }

        @Test
        void testWarnMethods() {
            String message = "test message";

            loggerWrapper.warn(message);
            loggerWrapper.warn(message, arg);
            loggerWrapper.warn(message, arg, arg2);
            loggerWrapper.warn(message, new Object[]{arg, arg2});
            loggerWrapper.warn(message, throwable);

            verify(mockLogger).warn(message);
            verify(mockLogger).warn(message, arg);
            verify(mockLogger).warn(message, arg, arg2);
            verify(mockLogger).warn(message, new Object[]{arg, arg2});
            verify(mockLogger).warn(message, throwable);
        }

        @Test
        void testErrorMethods() {
            String message = "test message";

            loggerWrapper.error(message);
            loggerWrapper.error(message, arg);
            loggerWrapper.error(message, arg, arg2);
            loggerWrapper.error(message, new Object[]{arg, arg2});
            loggerWrapper.error(message, throwable);

            verify(mockLogger).error(message);
            verify(mockLogger).error(message, arg);
            verify(mockLogger).error(message, arg, arg2);
            verify(mockLogger).error(message, new Object[]{arg, arg2});
            verify(mockLogger).error(message, throwable);
        }
    }

    @Nested
    class MarkerLogging {
        private final Object arg = "arg";
        private final Object arg2 = "arg2";
        private final Throwable throwable = new RuntimeException("test exception");

        @Test
        void testTraceWithMarker() {
            String message = "test message";

            loggerWrapper.trace(mockMarker, message);
            loggerWrapper.trace(mockMarker, message, arg);
            loggerWrapper.trace(mockMarker, message, arg, arg2);
            loggerWrapper.trace(mockMarker, message, new Object[]{arg, arg2});
            loggerWrapper.trace(mockMarker, message, throwable);

            verify(mockLogger).trace(eq(mockMarker), eq(message));
            verify(mockLogger).trace(eq(mockMarker), eq(message), eq(arg));
            verify(mockLogger).trace(eq(mockMarker), eq(message), eq(arg), eq(arg2));
            verify(mockLogger).trace(eq(mockMarker), eq(message), eq(new Object[]{arg, arg2}));
            verify(mockLogger).trace(eq(mockMarker), eq(message), eq(throwable));
        }

        @Test
        void testDebugWithMarker() {
            String message = "test message";

            loggerWrapper.debug(mockMarker, message);
            loggerWrapper.debug(mockMarker, message, arg);
            loggerWrapper.debug(mockMarker, message, arg, arg2);
            loggerWrapper.debug(mockMarker, message, new Object[]{arg, arg2});
            loggerWrapper.debug(mockMarker, message, throwable);

            verify(mockLogger).debug(eq(mockMarker), eq(message));
            verify(mockLogger).debug(eq(mockMarker), eq(message), eq(arg));
            verify(mockLogger).debug(eq(mockMarker), eq(message), eq(arg), eq(arg2));
            verify(mockLogger).debug(eq(mockMarker), eq(message), eq(new Object[]{arg, arg2}));
            verify(mockLogger).debug(eq(mockMarker), eq(message), eq(throwable));
        }

        @Test
        void testInfoWithMarker() {
            String message = "test message";

            loggerWrapper.info(mockMarker, message);
            loggerWrapper.info(mockMarker, message, arg);
            loggerWrapper.info(mockMarker, message, arg, arg2);
            loggerWrapper.info(mockMarker, message, new Object[]{arg, arg2});
            loggerWrapper.info(mockMarker, message, throwable);

            verify(mockLogger).info(eq(mockMarker), eq(message));
            verify(mockLogger).info(eq(mockMarker), eq(message), eq(arg));
            verify(mockLogger).info(eq(mockMarker), eq(message), eq(arg), eq(arg2));
            verify(mockLogger).info(eq(mockMarker), eq(message), eq(new Object[]{arg, arg2}));
            verify(mockLogger).info(eq(mockMarker), eq(message), eq(throwable));
        }

        @Test
        void testWarnWithMarker() {
            String message = "test message";

            loggerWrapper.warn(mockMarker, message);
            loggerWrapper.warn(mockMarker, message, arg);
            loggerWrapper.warn(mockMarker, message, arg, arg2);
            loggerWrapper.warn(mockMarker, message, new Object[]{arg, arg2});
            loggerWrapper.warn(mockMarker, message, throwable);

            verify(mockLogger).warn(eq(mockMarker), eq(message));
            verify(mockLogger).warn(eq(mockMarker), eq(message), eq(arg));
            verify(mockLogger).warn(eq(mockMarker), eq(message), eq(arg), eq(arg2));
            verify(mockLogger).warn(eq(mockMarker), eq(message), eq(new Object[]{arg, arg2}));
            verify(mockLogger).warn(eq(mockMarker), eq(message), eq(throwable));
        }

        @Test
        void testErrorWithMarker() {
            String message = "test message";

            loggerWrapper.error(mockMarker, message);
            loggerWrapper.error(mockMarker, message, arg);
            loggerWrapper.error(mockMarker, message, arg, arg2);
            loggerWrapper.error(mockMarker, message, new Object[]{arg, arg2});
            loggerWrapper.error(mockMarker, message, throwable);

            verify(mockLogger).error(eq(mockMarker), eq(message));
            verify(mockLogger).error(eq(mockMarker), eq(message), eq(arg));
            verify(mockLogger).error(eq(mockMarker), eq(message), eq(arg), eq(arg2));
            verify(mockLogger).error(eq(mockMarker), eq(message), eq(new Object[]{arg, arg2}));
            verify(mockLogger).error(eq(mockMarker), eq(message), eq(throwable));
        }
    }

    @Test
    void testErrorWithErrorCodeException() {
        String message = "test message";
        TestLowerException cause = new TestLowerException();
        TestUpperException errorCodeException = new TestUpperException(cause);

        loggerWrapper.error(message, errorCodeException);

        String expectedPrefix = String.format(LoggerWrapper.ERROR_CODE_LOG_PREFIX_TEMPLATE,
                errorCodeException.getErrorCode().getCode(),
                errorCodeException.getId());
        verify(mockLogger).error(eq(expectedPrefix + message), eq(errorCodeException));
    }
}

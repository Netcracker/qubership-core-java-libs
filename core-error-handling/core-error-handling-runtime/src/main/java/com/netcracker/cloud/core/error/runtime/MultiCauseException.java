package org.qubership.cloud.core.error.runtime;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Exception class with support for multiple independent causes.<br>
 * Causes are packed as Suppressed exceptions. This allows to display them in log messages in tree format
 */
public class MultiCauseException extends ErrorCodeException {
    public static final ErrorCode DEFAULT_ERROR_CODE = new ErrorCodeHolder("NC-COMMON-2100", "multi-cause error");

    public MultiCauseException(Collection<? extends ErrorCodeException> causeExceptions) {
        super(DEFAULT_ERROR_CODE, "multiple independent errors have happened");
        causeExceptions.forEach(this::addSuppressed);
    }

    public MultiCauseException(ErrorCode errorCode, String message,
                               Collection<? extends ErrorCodeException> causeExceptions) {
        super(errorCode, message);
        causeExceptions.forEach(this::addSuppressed);
    }

    public List<ErrorCodeException> getCauseExceptions() {
        return Arrays.stream(getSuppressed()).filter(e -> e instanceof ErrorCodeException)
                .map(e -> (ErrorCodeException) e)
                .collect(Collectors.toList());
    }
}

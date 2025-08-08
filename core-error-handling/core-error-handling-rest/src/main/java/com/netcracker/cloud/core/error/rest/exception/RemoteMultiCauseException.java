package org.qubership.cloud.core.error.rest.exception;

import org.qubership.cloud.core.error.runtime.ErrorCode;
import org.qubership.cloud.core.error.runtime.ErrorCodeException;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class RemoteMultiCauseException extends RemoteCodeException {

    public RemoteMultiCauseException(ErrorCode errorCode, String message,
                                     List<? extends RemoteCodeException> causeExceptions) {
        super(errorCode, message);
        causeExceptions.forEach(this::addSuppressed);
    }

    public List<ErrorCodeException> getCauseExceptions() {
        return Arrays.stream(getSuppressed()).filter(e -> e instanceof ErrorCodeException)
                .map(e -> (ErrorCodeException) e)
                .collect(Collectors.toList());
    }

    public List<String> getCauseErrorIds() {
        return getCauseExceptions().stream()
                .map(ErrorCodeException::getId)
                .collect(Collectors.toList());
    }
}

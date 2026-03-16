package com.netcracker.cloud.core.error.rest.exception;

import com.netcracker.cloud.core.error.runtime.ErrorCode;
import com.netcracker.cloud.core.error.runtime.ErrorCodeException;

import java.util.Arrays;
import java.util.List;

public class RemoteMultiCauseException extends RemoteCodeException {

    public RemoteMultiCauseException(ErrorCode errorCode, String message,
                                     List<? extends RemoteCodeException> causeExceptions) {
        super(errorCode, message);
        causeExceptions.forEach(this::addSuppressed);
    }

    public List<ErrorCodeException> getCauseExceptions() {
        return Arrays.stream(getSuppressed()).filter(ErrorCodeException.class::isInstance)
                .map(e -> (ErrorCodeException) e)
                .toList();
    }

    public List<String> getCauseErrorIds() {
        return getCauseExceptions().stream()
                .map(ErrorCodeException::getId)
                .toList();
    }
}

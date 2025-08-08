package org.qubership.cloud.core.error.rest.tmf;

import org.qubership.cloud.core.error.rest.exception.RemoteCodeException;
import org.qubership.cloud.core.error.rest.exception.RemoteMultiCauseException;
import org.qubership.cloud.core.error.runtime.ErrorCode;
import org.qubership.cloud.core.error.runtime.ErrorCodeHolder;
import lombok.NonNull;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class DefaultTmfErrorResponseConverter implements TmfErrorResponseConverter {

    public RemoteCodeException buildErrorCodeException(@NonNull TmfErrorResponse response) {
        ErrorCode errorCode = new ErrorCodeHolder(response.getCode(), response.getReason());
        List<TmfError> errors = response.getErrors();
        RemoteCodeException errorCodeException;
        if (errors != null && !errors.isEmpty()) {
            List<RemoteCodeException> causes =
                    response.getErrors().stream().map(this::buildException).collect(Collectors.toList());
            errorCodeException = new RemoteMultiCauseException(errorCode, response.getDetail(), causes);
        } else {
            errorCodeException = new RemoteCodeException(errorCode, response.getDetail());
        }
        errorCodeException.setId(response.getId());
        Optional.ofNullable(response.getStatus()).map(Integer::parseInt).ifPresent(errorCodeException::setStatus);
        Optional.ofNullable(response.getSource()).ifPresent(errorCodeException::setSource);
        Optional.ofNullable(response.getMeta()).ifPresent(meta -> errorCodeException.getMeta().putAll(meta));
        errorCodeException.setStackTrace(new StackTraceElement[]{});
        return errorCodeException;
    }

    public RemoteCodeException buildException(@NonNull TmfError error) {
        ErrorCode errorCode = new ErrorCodeHolder(error.getCode(), error.getReason());
        RemoteCodeException errorCodeException = new RemoteCodeException(errorCode, null);
        errorCodeException.setId(error.getId());
        Optional.ofNullable(error.getStatus()).map(Integer::parseInt).ifPresent(errorCodeException::setStatus);
        Optional.ofNullable(error.getSource()).ifPresent(errorCodeException::setSource);
        Optional.ofNullable(error.getMeta()).ifPresent(meta -> errorCodeException.getMeta().putAll(meta));
        errorCodeException.setStackTrace(new StackTraceElement[]{});
        return errorCodeException;
    }
}

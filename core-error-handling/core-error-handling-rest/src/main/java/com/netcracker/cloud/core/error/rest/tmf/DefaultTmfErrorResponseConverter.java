package com.netcracker.cloud.core.error.rest.tmf;

import com.netcracker.cloud.core.error.rest.exception.RemoteCodeException;
import com.netcracker.cloud.core.error.rest.exception.RemoteMultiCauseException;
import com.netcracker.cloud.core.error.runtime.ErrorCode;
import com.netcracker.cloud.core.error.runtime.ErrorCodeHolder;
import lombok.NonNull;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class DefaultTmfErrorResponseConverter implements TmfErrorResponseConverter {

    public RemoteCodeException buildErrorCodeException(@NonNull TmfErrorResponse response) {
        ErrorCode errorCode = new ErrorCodeHolder(response.getCode(), response.getReason());
        List<TmfError> errors = response.getErrors();
        RemoteCodeException errorCodeException;
        if (errors != null && !errors.isEmpty()) {
            List<RemoteCodeException> causes =
                    response.getErrors().stream().map(this::buildException).toList();
            errorCodeException = new RemoteMultiCauseException(errorCode, response.getDetail(), causes);
        } else {
            errorCodeException = new RemoteCodeException(errorCode, response.getDetail());
        }
        return getRemoteCodeException(errorCodeException, response.getId(), response.getStatus(), response.getSource(), response.getMeta());
    }

    public RemoteCodeException buildException(@NonNull TmfError error) {
        ErrorCode errorCode = new ErrorCodeHolder(error.getCode(), error.getReason());
        RemoteCodeException errorCodeException = new RemoteCodeException(errorCode, null);
        return getRemoteCodeException(errorCodeException, error.getId(), error.getStatus(), error.getSource(), error.getMeta());
    }

    private RemoteCodeException getRemoteCodeException(RemoteCodeException errorCodeException, String id, String status, Object source, Map<String, Object> meta2) {
        errorCodeException.setId(id);
        Optional.ofNullable(status).map(Integer::parseInt).ifPresent(errorCodeException::setStatus);
        Optional.ofNullable(source).ifPresent(errorCodeException::setSource);
        Optional.ofNullable(meta2).ifPresent(meta -> errorCodeException.getMeta().putAll(meta));
        errorCodeException.setStackTrace(new StackTraceElement[]{});
        return errorCodeException;
    }
}

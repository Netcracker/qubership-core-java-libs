package com.netcracker.cloud.core.error.rest.tmf;

import com.netcracker.cloud.core.error.rest.exception.RemoteCodeException;

public interface TmfErrorResponseConverter {
    RemoteCodeException buildErrorCodeException(TmfErrorResponse response);
}

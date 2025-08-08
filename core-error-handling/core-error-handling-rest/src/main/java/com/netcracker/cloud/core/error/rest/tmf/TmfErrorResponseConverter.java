package org.qubership.cloud.core.error.rest.tmf;

import org.qubership.cloud.core.error.rest.exception.RemoteCodeException;

public interface TmfErrorResponseConverter {
    RemoteCodeException buildErrorCodeException(TmfErrorResponse response);
}

package org.qubership.cloud.disableapi.quarkus;

import org.qubership.cloud.core.error.rest.tmf.TmfErrorResponse;
import org.qubership.cloud.disableapi.DeprecatedApiException;
import io.netty.handler.codec.http.HttpResponseStatus;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;

@Slf4j
public class ErrorHandler {
    public TmfErrorResponse buildErrorResponse(String method, String path, String antPath, Set<String> methods) {
        DeprecatedApiException e = new DeprecatedApiException(method, path, methods, antPath);
        log.warn(e.getDetail());
        HttpResponseStatus httpStatus = HttpResponseStatus.NOT_FOUND;
        return TmfErrorResponse.builder(e).status(String.valueOf(httpStatus.code())).build();
    }
}

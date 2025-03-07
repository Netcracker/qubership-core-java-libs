package org.qubership.cloud.bluegreen.impl.http.error;

import org.qubership.cloud.bluegreen.impl.http.ResponseHandler;

public class ErrorCodeException extends DefaultErrorCodeException {
    public final ResponseHandler<?> responseHandler;

    public ErrorCodeException(int httpCode, ResponseHandler<?> responseHandler, ResponseHandler<String> defaultErrorConsumer) {
        super(httpCode, defaultErrorConsumer);
        this.responseHandler = responseHandler;
    }
}

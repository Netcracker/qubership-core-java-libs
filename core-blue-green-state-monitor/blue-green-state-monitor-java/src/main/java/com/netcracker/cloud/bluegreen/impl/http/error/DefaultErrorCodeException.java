package com.netcracker.cloud.bluegreen.impl.http.error;

import com.netcracker.cloud.bluegreen.impl.http.ResponseHandler;
import lombok.Getter;

@Getter
public class DefaultErrorCodeException extends RuntimeException {
    private final ResponseHandler<String> defaultErrorConsumer;
    private final int httpCode;

    public DefaultErrorCodeException(int httpCode, ResponseHandler<String> errorConsumer) {
        super("Response code: " + httpCode + ". Body: " + errorConsumer.getBody());
        this.httpCode = httpCode;
        this.defaultErrorConsumer = errorConsumer;
    }
}

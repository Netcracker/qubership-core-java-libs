package com.netcracker.cloud.bluegreen.impl.http;

import com.fasterxml.jackson.core.type.TypeReference;
import com.netcracker.cloud.bluegreen.impl.util.JsonUtil;
import lombok.Getter;

import java.net.http.HttpHeaders;

public class ResponseHandler<T> {
    private Class<T> type;
    private TypeReference<T> typeRef;
    @Getter
    private T body;
    @Getter
    private HttpHeaders responseHeaders;

    public ResponseHandler(Class<T> type) {
        this.type = type;
    }

    public ResponseHandler(TypeReference<T> typeRef) {
        this.typeRef = typeRef;
    }

    public ResponseHandler<T> apply(String body, HttpHeaders headers) {
        if (this.type != null && this.type == String.class ||
                this.typeRef != null && this.typeRef.getType() == new TypeReference<String>() {
                }.getType()) {
            this.body = (T) body;
        } else {
            this.body = this.type != null ? JsonUtil.fromJson(body, this.type) : JsonUtil.fromJson(body, this.typeRef);
        }
        this.responseHeaders = headers;
        return this;
    }
}

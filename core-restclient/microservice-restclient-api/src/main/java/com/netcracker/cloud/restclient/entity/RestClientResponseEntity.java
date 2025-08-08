package com.netcracker.cloud.restclient.entity;

import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Data
@RequiredArgsConstructor
public class RestClientResponseEntity<T> {
    private final T responseBody;
    private final int httpStatus;
    private Map<String, List<String>> headers = new HashMap<>();

    public RestClientResponseEntity(T responseBody,
                                    int httpStatus,
                                    Map<String, List<String>> headers) {
        this.responseBody = responseBody;
        this.httpStatus = httpStatus;
        this.headers = headers;
    }

    @Override
    public int hashCode() {
        return Objects.hash(responseBody, httpStatus, headers);
    }

    @Override
    public boolean equals(Object o){
        if (o == this) {
            return true;
        }

        if (!(o instanceof RestClientResponseEntity)) {
            return false;
        }

        RestClientResponseEntity<T> restClientResponseEntity = (RestClientResponseEntity<T>) o;

        if(!(this.responseBody.equals(restClientResponseEntity.getResponseBody()) && this.httpStatus == restClientResponseEntity.getHttpStatus()))
            return false;
        else {
            if (headers.size() != restClientResponseEntity.getHeaders().size())
                return false;
            return headers.entrySet().stream().allMatch(entity -> entity.getValue().equals(restClientResponseEntity.getHeaders().get(entity.getKey())));
        }
    }
}

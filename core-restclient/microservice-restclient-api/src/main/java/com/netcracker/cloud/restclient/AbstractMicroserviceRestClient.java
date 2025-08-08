package com.netcracker.cloud.restclient;

import com.netcracker.cloud.restclient.entity.RestClientResponseEntity;

import java.net.URI;
import java.util.List;
import java.util.Map;

public abstract class AbstractMicroserviceRestClient implements MicroserviceRestClient {

    @Override
    public <T> RestClientResponseEntity<T> doRequest(String url,
                                                     HttpMethod httpMethod,
                                                     Map<String, List<String>> headers,
                                                     Object requestBody,
                                                     Class<T> responseClass) {
        return doRequest(URI.create(url),
                httpMethod,
                headers,
                requestBody,
                responseClass);
    }
}

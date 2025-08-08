package com.netcracker.cloud.restclient;


import org.qubership.cloud.restclient.entity.RestClientResponseEntity;

import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.util.List;
import java.util.Map;

public interface MicroserviceRestClient {

    <T> RestClientResponseEntity<T> doRequest(String url,
                                              HttpMethod httpMethod,
                                              @Nullable Map<String, List<String>> headers,
                                              @Nullable Object requestBody,
                                              Class<T> responseClass,
                                              Map<String, Object> params);

    <T> RestClientResponseEntity<T> doRequest(String url,
                                              HttpMethod httpMethod,
                                              @Nullable Map<String, List<String>> headers,
                                              @Nullable Object requestBody,
                                              Class<T> responseClass);

    <T> RestClientResponseEntity<T> doRequest(URI uri,
                                              HttpMethod httpMethod,
                                              @Nullable Map<String, List<String>> headers,
                                              @Nullable Object requestBody,
                                              Class<T> responseClass);
}

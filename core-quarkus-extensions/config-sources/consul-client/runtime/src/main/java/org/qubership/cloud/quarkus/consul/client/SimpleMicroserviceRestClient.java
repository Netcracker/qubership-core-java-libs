package org.qubership.cloud.quarkus.consul.client;

import org.qubership.cloud.restclient.HttpMethod;
import org.qubership.cloud.restclient.MicroserviceRestClient;
import org.qubership.cloud.restclient.entity.RestClientResponseEntity;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.util.List;
import java.util.Map;

public class SimpleMicroserviceRestClient implements MicroserviceRestClient {
    private final Client client;

    public SimpleMicroserviceRestClient(Client client) {
        this.client = client;
    }

    @Override
    public <T> RestClientResponseEntity<T> doRequest(String url,
                                                     HttpMethod httpMethod,
                                                     @Nullable Map<String, List<String>> headers,
                                                     @Nullable Object requestBody,
                                                     Class<T> responseClass,
                                                     Map<String, Object> params) {
        return doRequest(UriBuilder.fromUri(url).build(), httpMethod, headers, requestBody, responseClass);
    }

    @Override
    public <T> RestClientResponseEntity<T> doRequest(String url,
                                                     HttpMethod httpMethod,
                                                     @Nullable Map<String, List<String>> headers,
                                                     @Nullable Object requestBody,
                                                     Class<T> responseClass) {
        return doRequest(UriBuilder.fromUri(url).build(), httpMethod, headers, requestBody, responseClass);
    }

    @Override
    public <T> RestClientResponseEntity<T> doRequest(URI uri,
                                                     HttpMethod httpMethod,
                                                     @Nullable Map<String, List<String>> headers,
                                                     @Nullable Object requestBody,
                                                     Class<T> responseClass) {
        return send(uri, httpMethod, headers, requestBody, responseClass);
    }

    private <T> RestClientResponseEntity<T> send(URI uri,
                                                 HttpMethod httpMethod,
                                                 @Nullable Map<String, List<String>> headers,
                                                 @Nullable Object requestBody,
                                                 Class<T> responseClass) {
        Invocation.Builder builder = client.target(uri).request();
        if (headers != null) {
            headers.forEach((headerName, headerValues) -> builder.header(headerName, firstOrNull(headerValues)));
        }
        T responseBody = null;
        int responseStatus = 0;
        if (httpMethod == HttpMethod.GET) {
            Response response = builder.get();
            responseBody = response.readEntity(responseClass);
            responseStatus = response.getStatus();
        } else if (httpMethod == HttpMethod.POST) {
            try (Response response = builder.post(Entity.entity(requestBody, MediaType.APPLICATION_JSON))) {
                responseBody = response.readEntity(responseClass);
                responseStatus = response.getStatus();
            }
        }
        return new RestClientResponseEntity<>(responseBody, responseStatus, null);
    }

    private String firstOrNull(List<String> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }

        return values.get(0);
    }
}

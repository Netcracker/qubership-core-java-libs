package com.netcracker.cloud.restclient.okhttp;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.netcracker.cloud.core.error.rest.exception.RemoteCodeException;
import com.netcracker.cloud.core.error.rest.tmf.DefaultTmfErrorResponseConverter;
import com.netcracker.cloud.core.error.rest.tmf.TmfErrorResponse;
import com.netcracker.cloud.core.error.rest.tmf.TmfErrorResponseConverter;
import com.netcracker.cloud.context.propagation.core.RequestContextPropagation;
import com.netcracker.cloud.restclient.AbstractMicroserviceRestClient;
import com.netcracker.cloud.restclient.HttpMethod;
import com.netcracker.cloud.restclient.entity.RestClientResponseEntity;
import com.netcracker.cloud.restclient.exception.MicroserviceRestClientException;
import com.netcracker.cloud.restclient.exception.MicroserviceRestClientResponseException;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
public class MicroserviceOkHttpRestClient extends AbstractMicroserviceRestClient {

    private static final String contentTypeHeader = "Content-Type";
    private final OkHttpClient client;

    @Getter
    @Setter
    private ObjectMapper mapper = new ObjectMapper()
            .setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Getter
    @Setter
    private TmfErrorResponseConverter converter = new DefaultTmfErrorResponseConverter();

    public MicroserviceOkHttpRestClient(OkHttpClient client) {
        this.client = client.newBuilder()
                .addInterceptor(chain -> {
                    Request.Builder requestBuilder = chain.request().newBuilder();
                    RequestContextPropagation.populateResponse((key, value) -> requestBuilder.header(key, String.valueOf(value)));
                    return chain.proceed(requestBuilder.build());
                })
                .build();
    }

    @Override
    public <T> RestClientResponseEntity<T> doRequest(String urlTemplate,
                                                     HttpMethod httpMethod,
                                                     Map<String, List<String>> headers,
                                                     Object requestBody,
                                                     Class<T> responseClass,
                                                     Map<String, Object> params) {
        String expandedUrl = expandUrl(urlTemplate, params);
        return doRequest(URI.create(expandedUrl), httpMethod, headers, requestBody, responseClass);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> RestClientResponseEntity<T> doRequest(URI uri,
                                                     HttpMethod httpMethod,
                                                     Map<String, List<String>> headers,
                                                     Object requestBody,
                                                     Class<T> responseClass) {
        Headers okHeaders = buildOkHeaders(headers);
        RequestBody okBody = buildRequestBody(requestBody, httpMethod, okHeaders);

        Request request = new Request.Builder()
                .url(uri.toString())
                .headers(okHeaders)
                .method(httpMethod.name(), okBody)
                .build();

        try (Response response = client.newCall(request).execute()) {
            int code = response.code();
            Map<String, List<String>> responseHeaders = response.headers().toMultimap();
            byte[] bodyBytes = response.body() != null ? response.body().bytes() : null;

            if (response.isSuccessful()) {
                T mapped = mapResponseBody(bodyBytes, responseClass);
                return new RestClientResponseEntity<>(mapped, code, responseHeaders);
            }
            throw buildResponseException(code, bodyBytes, responseHeaders);
        } catch (IOException e) {
            throw new MicroserviceRestClientException(e.getMessage(), e);
        }
    }

    private Headers buildOkHeaders(Map<String, List<String>> headers) {
        Headers.Builder builder = new Headers.Builder();
        if (headers != null) {
            headers.forEach((name, values) -> values.forEach(v -> builder.add(name, v)));
        }
        if (builder.get(contentTypeHeader) == null) {
            builder.set(contentTypeHeader, "application/json");
        }
        return builder.build();
    }

    private RequestBody buildRequestBody(Object requestBody, HttpMethod httpMethod, Headers okHeaders) {
        MediaType contentType = MediaType.parse(Objects.requireNonNull(okHeaders.get(contentTypeHeader)));
        if (requestBody == null) {
            boolean needsEmptyBody = HttpMethod.POST.equals(httpMethod)
                    || HttpMethod.PUT.equals(httpMethod)
                    || HttpMethod.PATCH.equals(httpMethod);
            return needsEmptyBody ? RequestBody.create(new byte[0], contentType) : null;
        }
        try {
            byte[] bytes;
            bytes = switch (requestBody) {
                case String s  -> s.getBytes();
                case byte[] b  -> b;
                default        -> mapper.writeValueAsBytes(requestBody);
            };
            return RequestBody.create(bytes, contentType);
        } catch (IOException e) {
            throw new MicroserviceRestClientException("Failed to serialize request body", e);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T mapResponseBody(byte[] bodyBytes, Class<T> responseClass) throws IOException {
        if (bodyBytes == null || bodyBytes.length == 0 || responseClass == Void.class) {
            return null;
        }
        if (responseClass == String.class) {
            return (T) new String(bodyBytes);
        }
        if (responseClass == byte[].class) {
            return (T) bodyBytes;
        }
        return mapper.readValue(bodyBytes, responseClass);
    }

    private MicroserviceRestClientResponseException buildResponseException(
            int code, byte[] bodyBytes, Map<String, List<String>> responseHeaders) {
        if (bodyBytes != null && bodyBytes.length > 0) {
            try {
                TmfErrorResponse tmf = mapper.readValue(bodyBytes, TmfErrorResponse.class);
                RemoteCodeException rce = converter.buildErrorCodeException(tmf);
                return new MicroserviceRestClientResponseException(
                        rce.getMessage(), rce, code, bodyBytes, responseHeaders);
            } catch (Exception ce) {
                log.warn("Failed to parse response as TMF error response, cause: {}", ce.getMessage());
                return new MicroserviceRestClientResponseException(
                        "Request failed with status " + code, ce, code, bodyBytes, responseHeaders);
            }
        }
        return new MicroserviceRestClientResponseException(
                "Request failed with status " + code, null, code, bodyBytes, responseHeaders);
    }

    private String expandUrl(String urlTemplate, Map<String, Object> params) {
        if (params == null || params.isEmpty()) {
            return urlTemplate;
        }
        String expanded = urlTemplate;
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            expanded = expanded.replace("{" + entry.getKey() + "}", String.valueOf(entry.getValue()));
        }
        return expanded;
    }
}

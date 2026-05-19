package com.netcracker.cloud.restclient.okhttp;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.netcracker.cloud.core.error.rest.exception.RemoteCodeException;
import com.netcracker.cloud.core.error.rest.tmf.DefaultTmfErrorResponseConverter;
import com.netcracker.cloud.core.error.rest.tmf.TmfErrorResponse;
import com.netcracker.cloud.core.error.rest.tmf.TmfErrorResponseConverter;
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

@Slf4j
public class MicroserviceOkHttpRestClient extends AbstractMicroserviceRestClient {

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
        this.client = client;
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
        Request.Builder requestBuilder = new Request.Builder().url(uri.toString());

        Headers.Builder okHeadersBuilder = new Headers.Builder();
        if (headers != null) {
            headers.forEach((name, values) -> values.forEach(value -> okHeadersBuilder.add(name, value)));
        }

        if (okHeadersBuilder.get("Content-Type") == null) {
            okHeadersBuilder.set("Content-Type", "application/json");
        }
        Headers okHeaders = okHeadersBuilder.build();
        requestBuilder.headers(okHeaders);

        RequestBody okBody = null;
        if (requestBody != null) {
            byte[] bodyBytes;
            try {
                if (requestBody instanceof String) {
                    bodyBytes = ((String) requestBody).getBytes();
                } else if (requestBody instanceof byte[]) {
                    bodyBytes = (byte[]) requestBody;
                } else {
                    bodyBytes = mapper.writeValueAsBytes(requestBody);
                }
            } catch (IOException e) {
                throw new MicroserviceRestClientException("Failed to serialize request body", e);
            }
            okBody = RequestBody.create(bodyBytes, MediaType.parse(okHeaders.get("Content-Type")));
        } else if (HttpMethod.POST.equals(httpMethod) || HttpMethod.PUT.equals(httpMethod) || HttpMethod.PATCH.equals(httpMethod)) {
            okBody = RequestBody.create(new byte[0], MediaType.parse(okHeaders.get("Content-Type")));
        }

        requestBuilder.method(httpMethod.name(), okBody);

        try (Response response = client.newCall(requestBuilder.build()).execute()) {
            int code = response.code();
            Map<String, List<String>> responseHeaders = response.headers().toMultimap();
            byte[] responseBodyBytes = null;
            if (response.body() != null) {
                responseBodyBytes = response.body().bytes();
            }

            if (response.isSuccessful()) {
                T mappedBody = null;
                if (responseBodyBytes != null && responseBodyBytes.length > 0 && responseClass != Void.class) {
                    if (responseClass == String.class) {
                        mappedBody = (T) new String(responseBodyBytes);
                    } else if (responseClass == byte[].class) {
                        mappedBody = (T) responseBodyBytes;
                    } else {
                        mappedBody = mapper.readValue(responseBodyBytes, responseClass);
                    }
                }
                return new RestClientResponseEntity<>(mappedBody, code, responseHeaders);
            } else {
                MicroserviceRestClientResponseException mce;
                try {
                    if (responseBodyBytes != null && responseBodyBytes.length > 0) {
                        TmfErrorResponse tmfErrorResponse = mapper.readValue(responseBodyBytes, TmfErrorResponse.class);
                        final RemoteCodeException remoteCodeException = converter.buildErrorCodeException(tmfErrorResponse);
                        mce = new MicroserviceRestClientResponseException(remoteCodeException.getMessage(),
                                remoteCodeException, code, responseBodyBytes, responseHeaders);
                    } else {
                        mce = new MicroserviceRestClientResponseException("Request failed with status " + code,
                                null, code, responseBodyBytes, responseHeaders);
                    }
                } catch (Exception ce) {
                    log.warn("Failed to parse response as TMF error response, cause: {}", ce.getMessage());
                    mce = new MicroserviceRestClientResponseException("Request failed with status " + code,
                            ce, code, responseBodyBytes, responseHeaders);
                }
                throw mce;
            }
        } catch (IOException e) {
            throw new MicroserviceRestClientException(e.getMessage(), e);
        }
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

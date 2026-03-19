package com.netcracker.cloud.restclient.resttemplate;

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
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactoryBuilder;

import org.springframework.http.*;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;

@Slf4j
public class MicroserviceRestTemplate extends AbstractMicroserviceRestClient {
    private final RestTemplate restTemplate;
    @Getter
    @Setter
    private ObjectMapper mapper = new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL).configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    @Getter
    @Setter
    private TmfErrorResponseConverter converter = new DefaultTmfErrorResponseConverter();

    public MicroserviceRestTemplate() {        this(new RestTemplate());
    }

    public MicroserviceRestTemplate(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        this.restTemplate.setErrorHandler(new DefaultResponseErrorHandler() {
            @Override
            protected boolean hasError(HttpStatusCode unknownStatusCode) {
                int statusCode = unknownStatusCode.value();
                HttpStatus.Series series = HttpStatus.Series.resolve(statusCode);
                return series == null || series == HttpStatus.Series.CLIENT_ERROR || series == HttpStatus.Series.SERVER_ERROR;
            }
        });
    }


    private static HttpHeaders setHttpHeaders(Map<String, List<String>> headers) {
        HttpHeaders httpHeaders = new HttpHeaders();
        if (headers != null) {
            httpHeaders.addAll(new HttpHeaders(new LinkedMultiValueMap<>(headers)));
        } else {
            httpHeaders.addAll(new HttpHeaders(new LinkedMultiValueMap<>()));
        }
        if (null == httpHeaders.getContentType()) {
            httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        }
        return httpHeaders;
    }

    @Override
    public <T> RestClientResponseEntity<T> doRequest(String uriTemplate,
                                                     HttpMethod httpMethod,
                                                     Map<String, List<String>> headers,
                                                     Object requestBody, Class<T> responseClass,
                                                     Map<String, Object> params) {
        return doRequest(restTemplate.getUriTemplateHandler().expand(uriTemplate, params),
                httpMethod,
                headers,
                requestBody,
                responseClass);
    }

    @Override
    public <T> RestClientResponseEntity<T> doRequest(URI uri,
                                                     HttpMethod httpMethod,
                                                     Map<String, List<String>> headers,
                                                     Object requestBody,
                                                     Class<T> responseClass) {
        try {
            HttpHeaders httpHeaders = setHttpHeaders(headers);
            HttpEntity<Object> requestEntity = new HttpEntity<>(requestBody, httpHeaders);
            ResponseEntity<T> responseEntity = restTemplate.exchange(uri,
                    convertHttpMethod(httpMethod),
                    requestEntity,
                    responseClass);
            return new RestClientResponseEntity<>(responseEntity.getBody(),
                    responseEntity.getStatusCodeValue(),
                    responseEntity.getHeaders());

        } catch (RestClientResponseException e) {
            // try to convert to TMF response
            MicroserviceRestClientResponseException mce;
            try {
                TmfErrorResponse tmfErrorResponse = mapper.readValue(e.getResponseBodyAsByteArray(), TmfErrorResponse.class);
                final RemoteCodeException remoteCodeException = converter.buildErrorCodeException(tmfErrorResponse);
                mce = new MicroserviceRestClientResponseException(remoteCodeException.getMessage(), remoteCodeException, e.getRawStatusCode(), e.getResponseBodyAsByteArray(), e.getResponseHeaders());
            } catch (Exception ce) {
                // failed to parse as TMF format, fallback to non-TMF response
                log.warn("Failed to parse response as TMF error response, cause: {}", ce.getMessage());
                mce = new MicroserviceRestClientResponseException(e.getMessage(), e, e.getRawStatusCode(), e.getResponseBodyAsByteArray(), e.getResponseHeaders());
            }
            throw mce;
        } catch (RestClientException e) {
            throw new MicroserviceRestClientException(e.getMessage(), e);
        }
    }

    private org.springframework.http.HttpMethod convertHttpMethod(HttpMethod httpMethod) {
        return org.springframework.http.HttpMethod.valueOf(httpMethod.name());
    }
}

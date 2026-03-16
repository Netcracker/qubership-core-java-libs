package com.netcracker.cloud.restclient.webclient;

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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.netty.http.client.HttpClient;
import reactor.util.retry.Retry;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@Slf4j
public class MicroserviceWebClient extends AbstractMicroserviceRestClient {
    private final WebClient webClient;
    private Retry retryPolicy = Retry.backoff(0, Duration.ZERO).filter(throwable -> false); //stub
    @Getter
    @Setter
    private ObjectMapper mapper = new ObjectMapper()
            .setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    @Getter
    @Setter
    private TmfErrorResponseConverter converter = new DefaultTmfErrorResponseConverter();

    public MicroserviceWebClient(WebClient webClient) {
        this.webClient = webClient;
    }

    /**
     * Can be used when client libraries rely on MicroserviceWebClient but want to avoid direct
     * dependency on Spring. WebClient in this case will be provided as a dependency by underlying
     * library/service.
     */
    public MicroserviceWebClient() {
        this.webClient = WebClient.builder().build();
    }

    public MicroserviceWebClient(HttpClient httpClient) {
        this.webClient = WebClient.builder().clientConnector(new ReactorClientHttpConnector(httpClient)).build();
    }

    public MicroserviceWebClient withRetry(Retry retryPolicy) {
        this.retryPolicy = retryPolicy;
        return this;
    }

    private static HttpHeaders setHttpHeaders(HttpHeaders httpHeaders, Map<String, List<String>> headers) {
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
                                                     Object requestBody,
                                                     Class<T> responseClass,
                                                     Map<String, Object> params) {
        return doRequestInternal(() -> webClient.method(convertHttpMethod(httpMethod)).uri(uriTemplate, params),
                requestBody, headers, responseClass);
    }

    @Override
    public <T> RestClientResponseEntity<T> doRequest(URI uri,
                                                     HttpMethod httpMethod,
                                                     Map<String, List<String>> headers,
                                                     Object requestBody,
                                                     Class<T> responseClass) {
        return doRequestInternal(() -> webClient.method(convertHttpMethod(httpMethod)).uri(uri),
                requestBody, headers, responseClass);
    }

    private <T> RestClientResponseEntity<T> doRequestInternal(Supplier<WebClient.RequestBodySpec> requestBodySpecSupplier,
                                                              Object requestBody,
                                                              Map<String, List<String>> headers,
                                                              Class<T> responseClass) {
        try {
            WebClient.RequestBodySpec requestBodySpec = requestBodySpecSupplier.get();
            requestBodySpec = requestBodySpec.headers(hs -> setHttpHeaders(hs, headers));
            WebClient.RequestHeadersSpec<?> requestSpec = requestBodySpec;
            if (requestBody != null) {
                requestSpec = requestBodySpec.bodyValue(requestBody);
            }
            ResponseEntity<T> responseEntity = requestSpec.retrieve()
                    .onRawStatus(status -> {
                                HttpStatus.Series series = HttpStatus.Series.resolve(status);
                                return (series == null || series == HttpStatus.Series.CLIENT_ERROR || series == HttpStatus.Series.SERVER_ERROR);
                            },
                            ClientResponse::createException)
                    .toEntity(responseClass)
                    .retryWhen(retryPolicy)
                    .block();
            if (responseEntity == null) {
                throw new MicroserviceRestClientException("Null HTTP response");
            }
            return new RestClientResponseEntity<>(responseEntity.getBody(),
                    responseEntity.getStatusCode().value(),
                    HeaderUtils.toMap(responseEntity.getHeaders()));
        } catch (WebClientException e) {
            throw processWebClientException(e);
        }
    }

    private MicroserviceRestClientException processWebClientException(WebClientException e) {
        if (e instanceof WebClientResponseException re) {
            // try to convert to TMF response
            MicroserviceRestClientResponseException mce;
            try {
                TmfErrorResponse tmfErrorResponse = mapper.readValue(re.getResponseBodyAsByteArray(), TmfErrorResponse.class);
                final RemoteCodeException remoteCodeException = converter.buildErrorCodeException(tmfErrorResponse);
                mce = new MicroserviceRestClientResponseException(remoteCodeException.getMessage(),
                        remoteCodeException,
                        re.getStatusCode().value(), re.getResponseBodyAsByteArray(), HeaderUtils.toMap(re.getHeaders()));
            } catch (Exception ce) {
                // failed to parse as TMF format, fallback to non-TMF response
                log.warn("Failed to parse response as TMF error response, cause: {}", ce.getMessage());
                mce = new MicroserviceRestClientResponseException(re.getMessage(), re, re.getStatusCode().value(), re.getResponseBodyAsByteArray(), HeaderUtils.toMap(re.getHeaders()));
            }
            return mce;
        } else {
            return new MicroserviceRestClientException(e.getMessage(), e);
        }
    }

    private org.springframework.http.HttpMethod convertHttpMethod(HttpMethod httpMethod) {
        return org.springframework.http.HttpMethod.valueOf(httpMethod.name());
    }
}

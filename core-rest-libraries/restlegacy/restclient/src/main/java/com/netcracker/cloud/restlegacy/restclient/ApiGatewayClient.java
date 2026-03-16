package com.netcracker.cloud.restlegacy.restclient;

import com.netcracker.cloud.restlegacy.restclient.error.ProxyErrorException;
import com.netcracker.cloud.restlegacy.restclient.retry.RetryTemplateBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.RestClientException;

import java.util.Set;

public class ApiGatewayClient extends RestClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApiGatewayClient.class);
    private final String baseUrl;
    private String gatewayUrl;
    @Value("${apigateway.url:http://internal-gateway-service:8080}")
    private String apiGatewayUrl;

    @Value("${apigateway.url-https:https://internal-gateway-service:8443}")
    private String apiGatewayHttpsUrl;

    public ApiGatewayClient(Integer apiVersion,
                            String appName,
                            RetryTemplate retryTemplate) {
        super(retryTemplate);
        this.gatewayUrl = getGatewayUrl();
        this.baseUrl = "/api/v" + apiVersion + "/" + appName;
    }

    private String getGatewayUrl() {
        if (this.gatewayUrl == null) {
            this.gatewayUrl = apiGatewayUrl;
        }
        return this.gatewayUrl;
    }

    public ApiGatewayClient(Integer apiVersion,
                            String appName) {
        this(apiVersion, appName, new RetryTemplateBuilder().withNeverRetryPolicy().build());
    }

    @Override
    public ResponseEntity post(final String relativeURL,
                               final Object object) throws RestClientException {
        LOGGER.debug("Post request for URI '{}' and object '{}'", relativeURL, object);
        return super.post(getGatewayUrl() + baseUrl + relativeURL, object, Object.class);
    }

    @Override
    public <T> ResponseEntity<T> post(final String relativeURL,
                                      final Object object,
                                      final Class<T> responseType) throws RestClientException {
        LOGGER.debug("Post request for URI '{}' and object '{}'. Response type is '{}'", relativeURL, object, responseType);
        return super.post(getGatewayUrl() + baseUrl + relativeURL, object, responseType);
    }

    @Override
    public ResponseEntity get(final String relativeURL) throws RestClientException {
        LOGGER.debug("Get request for URI '{}'", relativeURL);
        return super.get(getGatewayUrl() + baseUrl + relativeURL, Object.class);
    }

    @Override
    public <T> ResponseEntity<T> get(final String relativeURL,
                                     final Class<T> responseType) throws RestClientException {
        LOGGER.debug("Get request for URI '{}'", relativeURL);
        return super.get(getGatewayUrl() + baseUrl + relativeURL, responseType);
    }

    @Override
    public <T> ResponseEntity<T> get(final String relativeURL,
                                     final HttpEntity httpEntity,
                                     final Class<T> responseType) throws ProxyErrorException {
        LOGGER.debug("Get request for URI '{}' and httpEntity '{}'. Response type is '{}'", relativeURL, httpEntity, responseType);
        return super.get(getGatewayUrl() + baseUrl + relativeURL, httpEntity, responseType);
    }

    @Override
    public void put(final String relativeURL,
                    final Object request) throws RestClientException {
        LOGGER.debug("Put request for URI '{}'", relativeURL);
        super.put(getGatewayUrl() + baseUrl + relativeURL, request);
    }

    @Override
    public void delete(final String relativeURL) throws RestClientException {
        LOGGER.debug("Delete request for URI '{}'", relativeURL);
        super.delete(getGatewayUrl() + baseUrl + relativeURL);
    }

    @Override
    public <T> T patch(final String relativeURL, Object request, Class<T> responseType) throws RestClientException {
        LOGGER.debug("Patch request for URI '{}'", relativeURL);
        return super.patch(getGatewayUrl() + baseUrl + relativeURL, request, responseType);
    }

    @Override
    public Set<HttpMethod> options(String relativeURL) throws RestClientException {
        LOGGER.debug("Options request for URI '{}'", relativeURL);
        return super.options(getGatewayUrl() + baseUrl + relativeURL);
    }

    @Override
    public HttpHeaders head(String relativeURL) throws RestClientException {
        LOGGER.debug("Head request for URI '{}'", relativeURL);
        return super.head(getGatewayUrl() + baseUrl + relativeURL);
    }

}

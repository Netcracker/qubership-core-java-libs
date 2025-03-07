package org.qubership.cloud.restlegacy.restclient;

import org.qubership.cloud.restlegacy.restclient.error.ProxyRethrowException;
import org.qubership.cloud.restlegacy.restclient.error.RestClientExceptionPropagator;
import org.qubership.cloud.restlegacy.restclient.error.UtilException;
import org.qubership.cloud.restlegacy.resttemplate.RestTemplateFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.retry.RetryContext;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Set;


/**
 * Common REST client which holds reference to synchronous RestTemplate and provides CRUD methods
 * for arbitrary URI <p> All methods are executed according to retry policy provided in
 * RetryTemplate and throw RestClientException in case of unsuccessful response
 */
public class RestClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiGatewayClient.class);
    private final RetryTemplate retryTemplate;
    @Autowired
    @Qualifier("restTemplate")
    private RestTemplate restTemplate;
    @Autowired
    private RestTemplateFactory restTemplateFactory;

    @Autowired(required = false)
    private RestClientExceptionPropagator clientExceptionPropagator = new ProxyErrorExceptionPropagator();

    public RestClient(RetryTemplate retryTemplate) {
        this.retryTemplate = retryTemplate;
    }


    protected RestTemplate getRestTemplate() {
        return restTemplateFactory.getRestTemplate();
    }

    public void addClientHttpRequestInterceptor(ClientHttpRequestInterceptor interceptor) {
        restTemplate.getInterceptors().add(interceptor);
    }

    public ResponseEntity post(final String URL,
                               final Object object) throws RestClientException {
        LOGGER.debug("Post request for URI '{}' and object '{}'", URL, object);
        return post(URL, object, Object.class);
    }

    public <T> ResponseEntity<T> post(final String url,
                                      final Object object,
                                      final Class<T> responseType) throws RestClientException {
        return withRetry(url, () -> {
            LOGGER.debug("Post request for URI '{}'", url);
            return getRestTemplate().postForEntity(url, object, responseType);
        });
    }

    public ResponseEntity get(final String URL) throws RestClientException {
        LOGGER.debug("Get request for URI '{}'", URL);
        return get(URL, Object.class);
    }


    public <T> ResponseEntity<T> get(final String url,
                                     final Class<T> responseType) throws RestClientException {
        return withRetry(url, () -> {
            LOGGER.debug("Get request for URI '{}'", url);
            return getRestTemplate().getForEntity(url, responseType);
        });
    }

    public <T> ResponseEntity<T> get(String url, HttpEntity httpEntity, Class<T> responseType) {
        return withRetry(url, () -> {
            LOGGER.debug("Get request for URI '{}'", url);
            return this.getRestTemplate().exchange(url, HttpMethod.GET, httpEntity, responseType);
        });
    }

    public void put(final String url,
                    final Object request) throws RestClientException {
        withRetry(url, () -> {
            LOGGER.debug("Put request for URI '{}'", url);
            getRestTemplate().put(url, request);
            return null;
        });
    }

    public void delete(final String url) throws RestClientException {
        withRetry(url, () -> {
            LOGGER.debug("Delete request for URI '{}'", url);
            getRestTemplate().delete(url);
            return null;
        });
    }

    public <T> T patch(String url, Object request, Class<T> responseType)
            throws RestClientException {
        return withRetry(url, () -> {
            LOGGER.debug("Patch request for URI '{}'", url);
            return getRestTemplate().patchForObject(url, request, responseType);
        });
    }

    public Set<HttpMethod> options(String url) throws RestClientException {
        return withRetry(url, () -> {
            LOGGER.debug("Options request for URI '{}'", url);
            return getRestTemplate().optionsForAllow(url);
        });
    }

    public HttpHeaders head(String url) throws RestClientException {
        return withRetry(url, () -> {
            LOGGER.debug("Head request for URI '{}'", url);
            return getRestTemplate().headForHeaders(url);
        });
    }

    private <T, E extends Exception> T withRetry(String url,
                                                 UtilException.Supplier_WithException<T, E> job) throws E {
        return retryTemplate.execute(retryContext -> {
            try {
                retryContext.setAttribute(RetryContext.NAME, url);
                return job.get();
            } catch (Exception e) {
                clientExceptionPropagator.propagate(e, url);
            }
            return null;
        });
    }

    /**
     * @deprecated For legacy error handling model only. Will be removed when no usages of legacy model will be
     */
    @Deprecated
    private static class ProxyErrorExceptionPropagator implements RestClientExceptionPropagator {
        @Override
        public void propagate(Exception exception, String failedUrl) {
            throw ProxyRethrowException.buildProxyException(exception, failedUrl);
        }
    }
}
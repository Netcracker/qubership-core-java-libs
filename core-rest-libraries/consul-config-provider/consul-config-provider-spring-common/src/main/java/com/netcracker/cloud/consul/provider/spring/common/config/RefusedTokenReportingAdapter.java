package com.netcracker.cloud.consul.provider.spring.common.config;

import com.netcracker.cloud.consul.provider.spring.common.TokenRefusals;
import org.springframework.cloud.consul.ConsulClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.service.invoker.HttpExchangeAdapter;
import org.springframework.web.service.invoker.HttpRequestValues;

/**
 * Reports the ACL token of a request Consul answered with {@code 403 ACL not found}. The token comes from the
 * {@code X-Consul-Token} header of that request, so what is reported is what went on the wire, whatever the caller has
 * read or replaced since.
 *
 * <p>The seam sits below {@link ConsulClient} rather than around it: every method of that interface returns a
 * {@link ResponseEntity} and therefore passes through {@link #exchangeForEntity} or {@link #exchangeForBodilessEntity},
 * so a method added to it is covered without a change here. The exchanges that carry no status are delegated
 * untouched.
 */
class RefusedTokenReportingAdapter implements HttpExchangeAdapter {

    private static final int REJECTED = 403;

    private final HttpExchangeAdapter delegate;
    private final TokenRefusals refusals;

    RefusedTokenReportingAdapter(HttpExchangeAdapter delegate, TokenRefusals refusals) {
        this.delegate = delegate;
        this.refusals = refusals;
    }

    @Override
    public boolean supportsRequestAttributes() {
        return delegate.supportsRequestAttributes();
    }

    @Override
    public void exchange(HttpRequestValues requestValues) {
        delegate.exchange(requestValues);
    }

    @Override
    public HttpHeaders exchangeForHeaders(HttpRequestValues requestValues) {
        return delegate.exchangeForHeaders(requestValues);
    }

    @Override
    public <T> T exchangeForBody(HttpRequestValues requestValues, ParameterizedTypeReference<T> bodyType) {
        return delegate.exchangeForBody(requestValues, bodyType);
    }

    @Override
    public ResponseEntity<Void> exchangeForBodilessEntity(HttpRequestValues requestValues) {
        return report(requestValues, delegate.exchangeForBodilessEntity(requestValues));
    }

    @Override
    public <T> ResponseEntity<T> exchangeForEntity(HttpRequestValues requestValues, ParameterizedTypeReference<T> bodyType) {
        return report(requestValues, delegate.exchangeForEntity(requestValues, bodyType));
    }

    private <T> ResponseEntity<T> report(HttpRequestValues requestValues, ResponseEntity<T> response) {
        if (response.getStatusCode().value() == REJECTED) {
            String sentToken = requestValues.getHeaders().getFirst(ConsulClient.ACL_TOKEN_HEADER);
            if (sentToken != null && !sentToken.isEmpty()) {
                refusals.report(sentToken);
            }
        }
        return response;
    }
}

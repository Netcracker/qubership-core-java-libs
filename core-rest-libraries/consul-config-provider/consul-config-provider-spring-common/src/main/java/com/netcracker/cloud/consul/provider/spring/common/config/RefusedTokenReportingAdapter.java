package com.netcracker.cloud.consul.provider.spring.common.config;

import com.netcracker.cloud.consul.provider.spring.common.TokenRefusals;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.consul.ConsulClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.UnknownContentTypeException;
import org.springframework.web.service.invoker.HttpExchangeAdapter;
import org.springframework.web.service.invoker.HttpRequestValues;

import java.util.function.Supplier;

/**
 * Reports a request Consul answered with {@code 403} to the owner of the ACL token, which then checks whether Consul
 * still resolves the token.
 *
 * <p>A refusal arrives in one of two shapes, and both are read here. The client treats a 4xx as an ordinary response,
 * so the status can come back on a {@link ResponseEntity}; but Consul answers a refusal with {@code text/plain}, which
 * no converter maps to the body type the client asked for, and the status then arrives on an
 * {@link UnknownContentTypeException} instead. Reading only the first shape leaves a refused token unreported against
 * a real Consul.
 *
 * <p>The seam sits below {@link ConsulClient} rather than around it, so a method added to that interface is covered by
 * whichever of the four exchanges it uses.
 */
class RefusedTokenReportingAdapter implements HttpExchangeAdapter {

    private static final int REFUSED = 403;

    private static final Logger log = LoggerFactory.getLogger(RefusedTokenReportingAdapter.class);

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
        reporting(() -> {
            delegate.exchange(requestValues);
            return null;
        });
    }

    @Override
    public HttpHeaders exchangeForHeaders(HttpRequestValues requestValues) {
        return reporting(() -> delegate.exchangeForHeaders(requestValues));
    }

    @Override
    public <T> T exchangeForBody(HttpRequestValues requestValues, ParameterizedTypeReference<T> bodyType) {
        return reporting(() -> delegate.exchangeForBody(requestValues, bodyType));
    }

    @Override
    public ResponseEntity<Void> exchangeForBodilessEntity(HttpRequestValues requestValues) {
        return reporting(() -> delegate.exchangeForBodilessEntity(requestValues));
    }

    @Override
    public <T> ResponseEntity<T> exchangeForEntity(HttpRequestValues requestValues, ParameterizedTypeReference<T> bodyType) {
        return reporting(() -> delegate.exchangeForEntity(requestValues, bodyType));
    }

    private <T> T reporting(Supplier<T> exchange) {
        T result;
        try {
            result = exchange.get();
        } catch (UnknownContentTypeException e) {
            report(e.getStatusCode());
            throw e;
        } catch (RestClientResponseException e) {
            report(e.getStatusCode());
            throw e;
        }
        if (result instanceof ResponseEntity<?> response) {
            report(response.getStatusCode());
        }
        return result;
    }

    private void report(HttpStatusCode status) {
        if (status.value() != REFUSED) {
            return;
        }
        log.debug("Consul refused a request of this pod; reporting it to the owner of the ACL token");
        refusals.report();
    }
}

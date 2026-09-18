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
 * Reports the ACL token of a request Consul answered with {@code 403 ACL not found}. The token comes from the
 * {@code X-Consul-Token} header of that request, so what is reported is what went on the wire, whatever the storage
 * holds by the time the answer arrives.
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

    private static final int REJECTED = 403;

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
        reporting(requestValues, () -> {
            delegate.exchange(requestValues);
            return null;
        });
    }

    @Override
    public HttpHeaders exchangeForHeaders(HttpRequestValues requestValues) {
        return reporting(requestValues, () -> delegate.exchangeForHeaders(requestValues));
    }

    @Override
    public <T> T exchangeForBody(HttpRequestValues requestValues, ParameterizedTypeReference<T> bodyType) {
        return reporting(requestValues, () -> delegate.exchangeForBody(requestValues, bodyType));
    }

    @Override
    public ResponseEntity<Void> exchangeForBodilessEntity(HttpRequestValues requestValues) {
        return reporting(requestValues, () -> delegate.exchangeForBodilessEntity(requestValues));
    }

    @Override
    public <T> ResponseEntity<T> exchangeForEntity(HttpRequestValues requestValues, ParameterizedTypeReference<T> bodyType) {
        return reporting(requestValues, () -> delegate.exchangeForEntity(requestValues, bodyType));
    }

    private <T> T reporting(HttpRequestValues requestValues, Supplier<T> exchange) {
        T result;
        try {
            result = exchange.get();
        } catch (UnknownContentTypeException e) {
            report(requestValues, e.getStatusCode());
            throw e;
        } catch (RestClientResponseException e) {
            report(requestValues, e.getStatusCode());
            throw e;
        }
        if (result instanceof ResponseEntity<?> response) {
            report(requestValues, response.getStatusCode());
        }
        return result;
    }

    private void report(HttpRequestValues requestValues, HttpStatusCode status) {
        if (status.value() != REJECTED) {
            return;
        }
        String sentToken = requestValues.getHeaders().getFirst(ConsulClient.ACL_TOKEN_HEADER);
        if (sentToken == null || sentToken.isEmpty()) {
            log.debug("Consul refused a request to {} that carried no ACL token", requestValues.getUriTemplate());
            return;
        }
        log.debug("Consul refused the ACL token this pod sent to {}; reporting it to the token owner",
                requestValues.getUriTemplate());
        refusals.report(sentToken);
    }
}

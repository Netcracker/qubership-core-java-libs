package com.netcracker.cloud.consul.provider.spring.common.config;

import com.netcracker.cloud.consul.provider.common.ConsulTokenSource;
import com.netcracker.cloud.consul.provider.spring.common.TokenRefusals;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.consul.ConsulClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.UnknownContentTypeException;
import org.springframework.web.service.invoker.HttpExchangeAdapter;
import org.springframework.web.service.invoker.HttpRequestValues;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RefusedTokenReportingAdapterTest {

    private static final ParameterizedTypeReference<String> STRING = new ParameterizedTypeReference<>() {
    };

    private final AtomicInteger reported = new AtomicInteger();

    private final TokenRefusals refusals = refusalsCountedBy(reported);

    /**
     * The shape a real Consul produces. It answers a refusal with {@code text/plain}, no converter maps that to the
     * body type the client asked for, and the status then reaches the adapter on the exception rather than on a
     * response. Reading only the response left a refused token unreported on the stand.
     */
    @Test
    void aRefusalThatArrivesAsAnUnreadableBodyIsReported() {
        HttpExchangeAdapter adapter = new RefusedTokenReportingAdapter(refusingWithPlainText(403), refusals);

        assertThrows(UnknownContentTypeException.class,
                () -> adapter.exchangeForEntity(requestWithToken("dead-token"), STRING));

        assertEquals(1, reported.get(), "reported refusals");
    }

    @Test
    void anUnreadableBodyUnderAnotherStatusIsNotReported() {
        HttpExchangeAdapter adapter = new RefusedTokenReportingAdapter(refusingWithPlainText(500), refusals);

        assertThrows(UnknownContentTypeException.class,
                () -> adapter.exchangeForEntity(requestWithToken("live-token"), STRING));

        assertEquals(0, reported.get(), "reported refusals");
    }

    @Test
    void aRefusalThatArrivesOnAResponseIsReported() {
        HttpExchangeAdapter adapter = new RefusedTokenReportingAdapter(answering(403), refusals);

        adapter.exchangeForEntity(requestWithToken("dead-token"), STRING);

        assertEquals(1, reported.get(), "reported refusals");
    }

    @Test
    void anAnswerOtherThanARefusalIsNotReported() {
        HttpExchangeAdapter adapter = new RefusedTokenReportingAdapter(answering(500), refusals);

        adapter.exchangeForEntity(requestWithToken("live-token"), STRING);

        assertEquals(0, reported.get(), "reported refusals");
    }

    @Test
    void aBodilessRefusalIsReportedToo() {
        HttpExchangeAdapter adapter = new RefusedTokenReportingAdapter(answering(403), refusals);

        adapter.exchangeForBodilessEntity(requestWithToken("dead-token"));

        assertEquals(1, reported.get(), "reported refusals");
    }

    @Test
    void anExchangeThatCarriesNoStatusIsDelegatedUntouched() {
        HttpExchangeAdapter adapter = new RefusedTokenReportingAdapter(answering(403), refusals);

        assertEquals("body", adapter.exchangeForBody(requestWithToken("dead-token"), STRING));
        assertEquals(0, reported.get(), "reported refusals");
    }

    private static HttpRequestValues requestWithToken(String token) {
        return HttpRequestValues.builder().addHeader(ConsulClient.ACL_TOKEN_HEADER, token).build();
    }

    private static TokenRefusals refusalsCountedBy(AtomicInteger reported) {
        TokenRefusals refusals = new TokenRefusals();
        refusals.reportTo(new ConsulTokenSource() {
            @Override
            public String get() {
                return "";
            }

            @Override
            public void reportRefusal() {
                reported.incrementAndGet();
            }
        });
        return refusals;
    }

    /**
     * Fails the exchange the way the Consul client does against a real Consul: its status handler lets a 4xx through,
     * and the body then fails to convert.
     */
    private static HttpExchangeAdapter refusingWithPlainText(int statusCode) {
        return new StubAdapter(statusCode) {
            @Override
            public <T> ResponseEntity<T> exchangeForEntity(HttpRequestValues requestValues,
                                                           ParameterizedTypeReference<T> bodyType) {
                throw new UnknownContentTypeException(bodyType.getType(), MediaType.TEXT_PLAIN,
                        HttpStatusCode.valueOf(statusCode), "Forbidden", HttpHeaders.EMPTY,
                        "ACL not found".getBytes(StandardCharsets.UTF_8));
            }
        };
    }

    private static HttpExchangeAdapter answering(int statusCode) {
        return new StubAdapter(statusCode);
    }

    private static class StubAdapter implements HttpExchangeAdapter {

        private final int statusCode;

        private StubAdapter(int statusCode) {
            this.statusCode = statusCode;
        }

        @Override
        public boolean supportsRequestAttributes() {
            return true;
        }

        @Override
        public void exchange(HttpRequestValues requestValues) {
            // nothing
        }

        @Override
        public HttpHeaders exchangeForHeaders(HttpRequestValues requestValues) {
            return HttpHeaders.EMPTY;
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T> T exchangeForBody(HttpRequestValues requestValues, ParameterizedTypeReference<T> bodyType) {
            return (T) "body";
        }

        @Override
        public ResponseEntity<Void> exchangeForBodilessEntity(HttpRequestValues requestValues) {
            return ResponseEntity.status(statusCode).build();
        }

        @Override
        public <T> ResponseEntity<T> exchangeForEntity(HttpRequestValues requestValues,
                                                       ParameterizedTypeReference<T> bodyType) {
            return ResponseEntity.status(statusCode).build();
        }
    }
}

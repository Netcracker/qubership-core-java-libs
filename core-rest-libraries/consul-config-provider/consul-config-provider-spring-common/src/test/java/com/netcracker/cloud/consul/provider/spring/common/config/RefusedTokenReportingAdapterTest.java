package com.netcracker.cloud.consul.provider.spring.common.config;

import com.netcracker.cloud.consul.provider.common.TokenStorage;
import com.netcracker.cloud.consul.provider.spring.common.TokenRefusals;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.consul.ConsulClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.service.invoker.HttpExchangeAdapter;
import org.springframework.web.service.invoker.HttpRequestValues;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RefusedTokenReportingAdapterTest {

    private final List<String> reported = new ArrayList<>();

    private final TokenRefusals refusals = refusalsRecordingInto(reported);

    @Test
    void aRefusedTokenIsReportedWithTheValueTheRequestCarried() {
        HttpExchangeAdapter adapter = new RefusedTokenReportingAdapter(answering(403), refusals);

        adapter.exchangeForEntity(requestWithToken("dead-token"), STRING);

        assertEquals(List.of("dead-token"), reported);
    }

    @Test
    void anAnswerOtherThanARefusalIsNotReported() {
        HttpExchangeAdapter adapter = new RefusedTokenReportingAdapter(answering(500), refusals);

        adapter.exchangeForEntity(requestWithToken("live-token"), STRING);

        assertTrue(reported.isEmpty(), "reported tokens");
    }

    @Test
    void aRefusalOfARequestThatCarriedNoTokenIsNotReported() {
        HttpExchangeAdapter adapter = new RefusedTokenReportingAdapter(answering(403), refusals);

        adapter.exchangeForEntity(HttpRequestValues.builder().build(), STRING);

        assertTrue(reported.isEmpty(), "reported tokens");
    }

    @Test
    void aBodilessRefusalIsReportedToo() {
        HttpExchangeAdapter adapter = new RefusedTokenReportingAdapter(answering(403), refusals);

        adapter.exchangeForBodilessEntity(requestWithToken("dead-token"));

        assertEquals(List.of("dead-token"), reported);
    }

    @Test
    void anExchangeThatCarriesNoStatusIsDelegatedUntouched() {
        HttpExchangeAdapter adapter = new RefusedTokenReportingAdapter(answering(403), refusals);

        assertEquals("body", adapter.exchangeForBody(requestWithToken("dead-token"), STRING));
        assertTrue(reported.isEmpty(), "reported tokens");
    }

    private static final ParameterizedTypeReference<String> STRING = new ParameterizedTypeReference<>() {
    };

    private static HttpRequestValues requestWithToken(String token) {
        return HttpRequestValues.builder().addHeader(ConsulClient.ACL_TOKEN_HEADER, token).build();
    }

    private static TokenRefusals refusalsRecordingInto(List<String> reported) {
        TokenRefusals refusals = new TokenRefusals();
        refusals.reportTo(new TokenStorage() {
            @Override
            public String get() {
                return "";
            }

            @Override
            public void update(String token) {
                // nothing
            }

            @Override
            public void invalidate(String rejectedToken) {
                reported.add(rejectedToken);
            }
        });
        return refusals;
    }

    /**
     * Answers every exchange with {@code statusCode}, which is what the Consul client sees: its own request factory
     * treats a 4xx as an ordinary response rather than an error.
     */
    private static HttpExchangeAdapter answering(int statusCode) {
        return new HttpExchangeAdapter() {

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
        };
    }
}

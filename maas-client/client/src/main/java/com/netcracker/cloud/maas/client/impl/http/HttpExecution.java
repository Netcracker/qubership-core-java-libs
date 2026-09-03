package com.netcracker.cloud.maas.client.impl.http;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.netcracker.cloud.maas.client.api.MaaSHttpException;
import com.netcracker.cloud.maas.client.impl.Env;
import dev.failsafe.Failsafe;
import dev.failsafe.FailsafeException;
import dev.failsafe.RetryPolicy;
import dev.failsafe.RetryPolicyBuilder;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;


@Slf4j
public class HttpExecution {
    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private final OkHttpClient httpClient;
    private final Request.Builder req;
    private final List<Integer> expectedCodes = new ArrayList<>();
    private final Map<Integer, Consumer<String>> errorHandler = new HashMap<>();
    private boolean retryEnabled = true;

    public static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    HttpExecution(OkHttpClient httpClient, Request.Builder req) {
        this.httpClient = httpClient;
        this.req = req;
    }

    public <T> HttpExecution post(T value) {
        try {
            this.req.post(RequestBody.create(MAPPER.writeValueAsBytes(value), JSON));
            return this;
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public <T> HttpExecution delete(T value) {
        try {
            this.req.delete(RequestBody.create(MAPPER.writeValueAsBytes(value), JSON));
            return this;
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public HttpExecution post(String body, String mediaType) {
        req.post(RequestBody.create(body, MediaType.parse(mediaType)));
        return this;
    }

    public HttpExecution expect(int... acceptableResponseCodes) {
        for (int acceptableResponseCode : acceptableResponseCodes) {
            expectedCodes.add(acceptableResponseCode);
        }
        return this;
    }

    public HttpExecution supressError(int code, Consumer<String> handler) {
        errorHandler.put(code, handler);
        return this;
    }

    /** Performs a single attempt. For callers that own a retry loop, such as a long poll. */
    public HttpExecution noRetry() {
        this.retryEnabled = false;
        return this;
    }

    private <R> Function<String, R> der(OmnivoreFunction<String, R> deserializer) {
        return body -> {
            try {
                return deserializer.apply(body);
            } catch (Exception e) {
                throw new RuntimeException("Error deserialize response: `" + body + "'", e);
            }
        };
    }

    public <R> Optional<R> sendAndReceive(Class<R> clazz) {
        return sendAndReceive()
                .filter(Predicate.not(String::isEmpty)) // MAPPER.readValue does not work for empty source
                .map(der(body -> MAPPER.readValue(body, clazz)));
    }

    public <R> Optional<R> sendAndReceive(TypeReference<R> typeRef) {
        return sendAndReceive()
                .filter(Predicate.not(String::isEmpty)) // MAPPER.readValue does not work for empty source
                .map(der(body -> MAPPER.readValue(body, typeRef)));
    }

    public <R> Optional<R> sendAndReceive(OmnivoreFunction<String, R> responseDeserializer) {
        return sendAndReceive().map(der(responseDeserializer));
    }

    /** Code carried by every maas-service TMF error envelope. */
    private static final String MAAS_ERROR_CODE = "MAAS-0600";

    /**
     * Whether the status is worth another attempt. 405 and 401 are here because maas-service
     * reports a read-only database as 405, and a token can expire in flight.
     */
    private static boolean isRetryableStatus(int code, String body) {
        if (code >= 500 || code == 429 || code == 401) {
            return true;
        }
        return code == 405 && isDatabaseUnavailable(body);
    }

    /** Tells the 405 of a read-only database apart from a plain one, which is permanent. */
    private static boolean isDatabaseUnavailable(String body) {
        if (body == null || !body.contains(MAAS_ERROR_CODE)) {
            return false;
        }
        String reason = body.toLowerCase(Locale.ROOT);
        return reason.contains("read-only") || reason.contains("read only")
                || reason.contains("not in 'active' mode");
    }

    /** One is enough: further attempts would re-send the same token. */
    static final int MAX_AUTH_RETRIES = 1;

    private int authAttempts = 0;

    /** Asked once per failed attempt, so the 401 is counted here. */
    private boolean worthAnotherAttempt(RetryableStatus status) {
        return status.code != 401 || ++authAttempts <= MAX_AUTH_RETRIES;
    }

    /** First backoff pause, and the fraction of the total duration a single pause may reach. */
    private static final Duration BASE_DELAY = Duration.ofSeconds(1);
    private static final int MAX_DELAY_FRACTION_OF_TOTAL = 4;
    private static final double JITTER = 0.2;

    // Response.body() is nullable in OkHttp; a missing body reads as empty.
    private static String bodyAsString(Response response) throws IOException {
        ResponseBody body = response.body();
        return body == null ? "" : body.string();
    }

    /** Body of a non-2xx response. Never throws: an unreadable body must not become a retry. */
    private static String errorBodyOrPlaceholder(Response response) {
        try {
            return bodyAsString(response);
        } catch (IOException e) {
            log.debug("Could not read error response body", e);
            return "<unreadable: " + e.getMessage() + ">";
        }
    }

    /** Sends the request, retrying under the policy below until it succeeds or runs out of time. */
    private Optional<String> sendAndReceive() {
        Request compiledReq = req.build();
        log.debug("Send request: {}", compiledReq);

        long maxTotalMillis = Env.httpRetryMaxTotalDuration().toMillis();
        if (!retryEnabled || maxTotalMillis <= 0) {
            return attemptOnce(compiledReq);
        }
        authAttempts = 0;
        try {
            return Failsafe.with(retryPolicy(compiledReq, maxTotalMillis)).get(context ->
                    attempt(compiledReq, maxTotalMillis - context.getElapsedTime().toMillis(), true));
        } catch (RetryableStatus | FailsafeException e) {
            Throwable last = e instanceof FailsafeException failsafe ? failsafe.getCause() : null;
            if (last instanceof InterruptedException interrupted) {
                // Failsafe restores the flag; the call still has to abort rather than retry
                Thread.currentThread().interrupt();
                throw new MaaSHttpException("Interrupted while waiting to retry maas-agent request", interrupted);
            }
            throw new MaaSHttpException("Gave up on " + compiledReq + ": ran out of its "
                    + maxTotalMillis + "ms total duration.\n\tLast attempt: "
                    + (last != null ? last : e), last);
        }
    }

    private RetryPolicy<Optional<String>> retryPolicy(Request compiledReq, long maxTotalMillis) {
        Duration maxDelay = Duration.ofMillis(Math.max(1, maxTotalMillis / MAX_DELAY_FRACTION_OF_TOTAL));
        RetryPolicyBuilder<Optional<String>> policy = RetryPolicy.builder();
        if (BASE_DELAY.compareTo(maxDelay) < 0) {
            policy.withBackoff(BASE_DELAY, maxDelay, 2.0);
        } else {
            policy.withDelay(maxDelay); // too short for the pause to grow
        }
        return policy
                .handle(IOException.class)
                .handleIf((ignored, failure) ->
                        failure instanceof RetryableStatus status && worthAnotherAttempt(status))
                .withJitter(JITTER)
                .withMaxAttempts(-1)
                .withMaxDuration(Duration.ofMillis(maxTotalMillis))
                .onRetry(event -> log.warn("Retrying request: {}. Attempt {} failed with {}, within {}ms total",
                        compiledReq, event.getAttemptCount(), event.getLastException(), maxTotalMillis))
                .build();
    }

    /** One request/response exchange. Throws {@link RetryableStatus} for an outcome worth repeating. */
    private Optional<String> attempt(Request compiledReq, long remainingMs, boolean retrying) throws IOException {
        Call call = httpClient.newCall(compiledReq);
        if (remainingMs > 0) {
            // an attempt starting near the deadline must not overrun the total duration
            call.timeout().timeout(remainingMs, TimeUnit.MILLISECONDS);
        }
        try (Response response = call.execute()) {
            // check response codes against acceptable list
            log.debug("Received status code: {}, expected codes: {}", response.code(), expectedCodes);

            if (errorHandler.containsKey(response.code())) {
                errorHandler.get(response.code()).accept(bodyAsString(response));
                return Optional.empty();
            }

            if (!expectedCodes.contains(response.code())) {
                String errorBody = errorBodyOrPlaceholder(response);
                if (retrying && isRetryableStatus(response.code(), errorBody)) {
                    throw new RetryableStatus(response.code(), errorBody);
                }
                throw new MaaSHttpException("Unexpected status code " + response.code()
                        + " for request: " + compiledReq + "\n\tResponse body: " + errorBody);
            }

            String body = bodyAsString(response);
            log.debug("Response body: {}", body);
            return Optional.of(body);
        }
    }

    /** The {@link #noRetry()} path, and a total duration configured to zero. */
    private Optional<String> attemptOnce(Request compiledReq) {
        try {
            return attempt(compiledReq, 0, false);
        } catch (IOException e) {
            throw new MaaSHttpException("Error executing " + compiledReq, e);
        }
    }

    /** A status the caller did not expect, but one worth another attempt. Never leaves this class. */
    private static final class RetryableStatus extends RuntimeException {
        private final transient int code;

        RetryableStatus(int code, String body) {
            super("status " + code + ", body: " + body, null, false, false);
            this.code = code;
        }

        @Override
        public String toString() {
            return getMessage();
        }
    }
}

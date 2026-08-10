package com.netcracker.cloud.maas.client.impl.http;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.netcracker.cloud.maas.client.impl.Env;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
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
     * Two 4xx are transient here rather than permanent: 405 is how maas-service reports a
     * read-only Postgres during a leader switchover, and 401 clears when the token is
     * re-supplied on the next attempt.
     * <p>
     * The 405 case is gated on the response body, because a plain 405 — a route removed on
     * the server, an ingress rejecting the method — is permanent and must fail fast.
     */
    private static boolean isRetryableStatus(int code, String body) {
        if (code >= 500) {
            return true;
        }
        if (code == 429 || code == 401) {
            return true;
        }
        return code == 405 && isDatabaseUnavailable(body);
    }

    /**
     * Recognises the 405 that maas-service returns for PostgreSQL error 25006, mapped from
     * {@code DatabaseIsReadonlyError} / {@code DatabaseIsNotActiveError}. Matched on the
     * reason text because the TMF code is the same for every maas-service error.
     */
    private static boolean isDatabaseUnavailable(String body) {
        if (body == null || !body.contains(MAAS_ERROR_CODE)) {
            return false;
        }
        return body.contains("read-only") || body.contains("not in 'active' mode");
    }

    /**
     * How many times a single call retries a 401. One is enough: it covers a token that
     * expired in flight. A token the supplier still considers valid but the server rejects
     * comes back identical on every further attempt.
     */
    static final int MAX_AUTH_RETRIES = 1;

    private int authAttempts = 0;

    private boolean canRetryStatus(int code, String body, long deadlineNanos) {
        if (!isRetryableStatus(code, body) || !canRetry(deadlineNanos)) {
            return false;
        }
        if (code == 401) {
            return ++authAttempts <= MAX_AUTH_RETRIES;
        }
        return true;
    }

    /** First backoff pause. */
    private static final long BASE_DELAY_MILLIS = 1_000L;

    /** A single pause is capped at this fraction of the total duration. */
    private static final int MAX_DELAY_FRACTION_OF_TOTAL = 4;

    /**
     * Delay before jitter: doubles per attempt, capped. Integer arithmetic saturating at
     * the cap, so a large attempt count cannot overflow.
     */
    static long cappedDelayMillis(int attempt, long maxTotalMillis) {
        long max = Math.max(1L, maxTotalMillis / MAX_DELAY_FRACTION_OF_TOTAL);
        long delay = Math.min(BASE_DELAY_MILLIS, max);
        for (int i = 1; i < attempt && delay < max; i++) {
            delay = delay > max / 2 ? max : delay * 2;
        }
        return delay;
    }

    // Exponential backoff with jitter between retries.
    private static long backoffMillis(int attempt, long maxTotalMillis) {
        long capped = cappedDelayMillis(attempt, maxTotalMillis);
        double jitterFactor = 0.8 + ThreadLocalRandom.current().nextDouble() * 0.4;
        return Math.max(1L, (long) (capped * jitterFactor));
    }

    // The total duration is the only stop condition, unless noRetry() was used.
    private boolean canRetry(long deadlineNanos) {
        return retryEnabled && System.nanoTime() < deadlineNanos;
    }

    /**
     * Waits before the next retry, clamped to what is left of the total duration so the
     * backoff cannot overshoot it. Restores the interrupt flag and aborts if interrupted.
     */
    private static void sleepBackoff(int attempt, long maxTotalMillis, long deadlineNanos) {
        long remaining = remainingMillis(deadlineNanos);
        if (remaining <= 0) {
            return;
        }
        try {
            Thread.sleep(Math.min(backoffMillis(attempt, maxTotalMillis), remaining));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting to retry maas-agent request", e);
        }
    }

    // Response.body() is nullable in OkHttp; a missing body reads as empty.
    private static String bodyAsString(Response response) throws IOException {
        ResponseBody body = response.body();
        return body == null ? "" : body.string();
    }

    /**
     * Body of a non-2xx response, for the retry decision and the error message. Never throws:
     * a body that cannot be read must not turn a permanent status into a retry.
     */
    private static String errorBodyOrPlaceholder(Response response) {
        try {
            return bodyAsString(response);
        } catch (IOException e) {
            log.debug("Could not read error response body", e);
            return "<unreadable: " + e.getMessage() + ">";
        }
    }

    private static long remainingMillis(long deadlineNanos) {
        return TimeUnit.NANOSECONDS.toMillis(deadlineNanos - System.nanoTime());
    }

    private static String giveUpSuffix(int attempt) {
        return attempt == 0 ? "" : ", gave up after " + attempt + " retries";
    }

    /**
     * Client for one attempt, bounded by what is left of the total duration. Without it an
     * attempt starting just before the deadline still runs for the full
     * {@code maas.http.timeout} and the call overruns its budget.
     * <p>
     * Not applied under {@link #noRetry()}: there the caller owns the lifecycle, and the
     * watch long poll legitimately runs as long as the budget itself.
     */
    private OkHttpClient clientForAttempt(long remainingMs) {
        if (!retryEnabled) {
            return httpClient;
        }
        // newBuilder shares the connection pool and dispatcher, so this is cheap
        return httpClient.newBuilder()
                .callTimeout(Duration.ofMillis(remainingMs))
                .build();
    }

    private Optional<String> sendAndReceive() {
        Request compiledReq = req.build();
        log.debug("Send request: {}", compiledReq);

        long maxTotalMillis = Env.httpRetryMaxTotalDuration().toMillis();
        long deadlineNanos = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(maxTotalMillis);
        int attempt = 0;
        while (true) {
            long remainingMs = remainingMillis(deadlineNanos);
            if (remainingMs <= 0) {
                throw new RuntimeException("Gave up on " + compiledReq + " after " + attempt
                        + " retries: the " + maxTotalMillis + "ms budget is spent");
            }

            try (Response response = clientForAttempt(remainingMs).newCall(compiledReq).execute()) {
                // check response codes against acceptable list
                log.debug("Received status code: {}, expected codes: {}", response.code(), expectedCodes);

                if (errorHandler.containsKey(response.code())) {
                    errorHandler.get(response.code()).accept(bodyAsString(response));
                    return Optional.empty();
                }

                if (!expectedCodes.contains(response.code())) {
                    // read once, without throwing: a body that cannot be read must not turn a
                    // permanent status into a retry
                    String errorBody = errorBodyOrPlaceholder(response);
                    if (canRetryStatus(response.code(), errorBody, deadlineNanos)) {
                        attempt++;
                        log.warn("Retryable status code {} for request: {}. Retry {}, within {}ms total",
                                response.code(), compiledReq, attempt, maxTotalMillis);
                        sleepBackoff(attempt, maxTotalMillis, deadlineNanos);
                        continue;
                    }
                    throw new RuntimeException("Unexpected status code " + response.code()
                            + " for request: " + compiledReq
                            + giveUpSuffix(attempt)
                            + "\n\tResponse body: " + errorBody);
                }

                String body = bodyAsString(response);
                log.debug("Response body: {}", body);
                return Optional.of(body);
            } catch (IOException e) {
                if (!canRetry(deadlineNanos)) {
                    throw new RuntimeException("Error executing " + compiledReq + giveUpSuffix(attempt), e);
                }
                attempt++;
                log.warn("Error execute http request: {}, Retry {}, within {}ms total",
                        e.getMessage(), attempt, maxTotalMillis);
                sleepBackoff(attempt, maxTotalMillis, deadlineNanos);
            }
        }
    }
}

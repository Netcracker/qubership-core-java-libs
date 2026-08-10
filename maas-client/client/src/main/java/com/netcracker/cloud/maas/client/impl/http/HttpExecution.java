package com.netcracker.cloud.maas.client.impl.http;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.netcracker.cloud.maas.client.impl.Env;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.io.IOException;
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

    /**
     * Performs a single attempt and lets the caller decide what to do with a failure.
     */
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

    /**
     * Two 4xx codes are deliberately retryable. Both are transient in this specific chain
     * rather than permanent client errors:
     * <ul>
     *   <li>405 - maas-service maps PG error 25006 (READ ONLY SQL TRANSACTION) to
     *       {@code StatusMethodNotAllowed}, so a write against a demoted Patroni node
     *       during a leader switchover arrives here as 405, not as 5xx.</li>
     *   <li>401 - the M2M token is supplied per request by the OkHttp interceptor, so an
     *       expired token or a briefly unavailable token provider resolves itself on the
     *       next attempt.</li>
     * </ul>
     */
    private static boolean isRetryableStatus(int code) {
        if (code >= 500) {
            return true;
        }
        return code == 429 || code == 405 || code == 401;
    }

    /** First backoff pause. Not configurable*/
    private static final long BASE_DELAY_MILLIS = 1_000L;

    /**
     * The cap on a single pause is derived from the total duration rather than configured
     * separately. A quarter keeps the growth useful at both ends of the range: with the
     * default 60s the pauses run 1s, 2s, 4s, 8s, 15s, 15s (~6 attempts), and with a 5s
     * total they run 1s, 1.25s, 1.25s, 1.25s (~4 attempts). Either way the caller gets
     * several tries without hammering an agent that is coming back up.
     */
    private static final int MAX_DELAY_FRACTION_OF_TOTAL = 4;

    /**
     * Delay before jitter: doubles per attempt, capped at the derived maximum.
     * Doubling is done in integer arithmetic and saturates at the cap, so a large attempt
     * count cannot overflow into a negative delay.
     */
    static long cappedDelayMillis(int attempt) {
        long max = Math.max(1L, Env.httpRetryMaxTotalDuration().toMillis() / MAX_DELAY_FRACTION_OF_TOTAL);
        long delay = Math.min(BASE_DELAY_MILLIS, max);
        for (int i = 1; i < attempt && delay < max; i++) {
            delay = delay > max / 2 ? max : delay * 2;
        }
        return delay;
    }

    // Exponential backoff with jitter between retries.
    private static long backoffMillis(int attempt) {
        long capped = cappedDelayMillis(attempt);
        double jitterFactor = 0.8 + ThreadLocalRandom.current().nextDouble() * 0.4;
        return Math.max(1L, (long) (capped * jitterFactor));
    }

    // The total duration is the only stop condition: there is no attempt counter to
    // disagree with it. Callers that own a retry loop opt out entirely via noRetry().
    private boolean canRetry(long deadlineNanos) {
        return retryEnabled && System.nanoTime() < deadlineNanos;
    }

    /**
     * Waits before the next retry. The delay is clamped to whatever is left of the max
     * total duration, otherwise a backoff started just before the deadline would overshoot
     * it by up to the configured max delay. Restores the interrupt flag and aborts if
     * interrupted.
     */
    private static void sleepBackoff(int attempt, long deadlineNanos) {
        long remainingMillis = TimeUnit.NANOSECONDS.toMillis(deadlineNanos - System.nanoTime());
        if (remainingMillis <= 0) {
            return;
        }
        try {
            Thread.sleep(Math.min(backoffMillis(attempt), remainingMillis));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting to retry maas-agent request", e);
        }
    }

    // OkHttp declares Response.body() as nullable; treat a missing body as empty rather
    // than dereferencing it. Keeps throwing IOException so the caller's retry loop sees it.
    private static String bodyAsString(Response response) throws IOException {
        ResponseBody body = response.body();
        return body == null ? "" : body.string();
    }

    private Optional<String> sendAndReceive() {
        Request compiledReq = req.build();
        log.debug("Send request: {}", compiledReq);

        long maxTotalMillis = Env.httpRetryMaxTotalDuration().toMillis();
        long deadlineNanos = System.nanoTime() + Env.httpRetryMaxTotalDuration().toNanos();
        int attempt = 0;
        while (true) {
            try (Response response = httpClient.newCall(compiledReq).execute()) {
                // check response codes against acceptable list
                log.debug("Received status code: {}, expected codes: {}", response.code(), expectedCodes);

                if (errorHandler.containsKey(response.code())) {
                    errorHandler.get(response.code()).accept(bodyAsString(response));
                    return Optional.empty();
                }

                if (!expectedCodes.contains(response.code())) {
                    if (isRetryableStatus(response.code()) && canRetry(deadlineNanos)) {
                        attempt++;
                        log.warn("Retryable status code {} for request: {}. Retry {}, within {}ms total",
                                response.code(), compiledReq, attempt, maxTotalMillis);
                        sleepBackoff(attempt, deadlineNanos);
                        continue;
                    }
                    throw new RuntimeException("Unexpected status code " + response.code()
                            + " for request: " + compiledReq
                            + ", gave up after " + attempt + " retries"
                            + "\n\tResponse body: " + bodyAsString(response));
                }

                String body = bodyAsString(response);
                log.debug("Response body: {}", body);
                return Optional.of(body);
            } catch (IOException e) {
                if (!canRetry(deadlineNanos)) {
                    throw new RuntimeException("Error executing " + compiledReq
                            + ", gave up after " + attempt + " retries", e);
                }
                attempt++;
                log.warn("Error execute http request: {}, Retry {}, within {}ms total",
                        e.getMessage(), attempt, maxTotalMillis);
                sleepBackoff(attempt, deadlineNanos);
            }
        }
    }
}

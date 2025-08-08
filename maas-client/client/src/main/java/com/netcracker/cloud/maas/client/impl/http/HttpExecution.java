package org.qubership.cloud.maas.client.impl.http;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;


@Slf4j
public class HttpExecution {
    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private final int RETRIES_NUMBER = 30;
    private final OkHttpClient httpClient;
    private final Request.Builder req;
    private final List<Integer> expectedCodes = new ArrayList<>();
    private final Map<Integer, Consumer<String>> errorHandler = new HashMap<>();

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

    @SneakyThrows
    private Optional<String> sendAndReceive() {
        Request compiledReq = req.build();
        log.debug("Send request: {}", compiledReq);

        int attempt = 0;
        while (true) {
            try (Response response = httpClient.newCall(compiledReq).execute()) {
                // check response codes against acceptable list
                log.debug("Received status code: {}, expected codes: {}", response.code(), expectedCodes);

                if (errorHandler.containsKey(response.code())) {
                    errorHandler.get(response.code()).accept(response.body().string());
                    return Optional.empty();
                }

                if (!expectedCodes.contains(response.code())) {
                    throw new RuntimeException("Unexpected status code " + response.code() + " for request: " + compiledReq + "\n\tResponse body: " + response.body().string());
                }

                String body = response.body().string();
                log.debug("Response body: {}", body);
                return Optional.of(body);
            } catch (IOException e) {
                if (attempt++ < RETRIES_NUMBER) {
                    log.warn("Error execute http request: {}, Retry {} of {}", e.getMessage(), attempt, RETRIES_NUMBER);
                    Thread.sleep(1000);
                } else {
                    throw new RuntimeException("Error executing " + compiledReq + ". Number of " + RETRIES_NUMBER + " retries exceeded", e);
                }
            }
        }
    }
}

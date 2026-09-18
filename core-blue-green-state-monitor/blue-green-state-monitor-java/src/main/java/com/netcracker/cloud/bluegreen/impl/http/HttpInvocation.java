package com.netcracker.cloud.bluegreen.impl.http;

import com.fasterxml.jackson.core.type.TypeReference;
import com.netcracker.cloud.bluegreen.impl.http.error.DefaultErrorCodeException;
import com.netcracker.cloud.bluegreen.impl.http.error.ErrorCodeException;
import com.netcracker.cloud.bluegreen.impl.http.error.InvocationException;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
public class HttpInvocation<R> {

    private static int ALL_ERRORS_CODE = 999;
    private static final int REFUSED = 403;

    private Class<R> type;
    private TypeReference<R> typeRef;
    private final HttpClient client;
    private final HttpRequest request;

    private final Runnable onRefusal;

    private final Set<Integer> successCodes;
    private final Map<Integer, ResponseHandler<?>> errorHandlersMap = new HashMap<>();

    public HttpInvocation(Class<R> type, HttpClient client, HttpRequest request, int... codes) {
        this(type, client, request, () -> {
        }, codes);
    }

    public HttpInvocation(TypeReference<R> typeReference, HttpClient client, HttpRequest request, int... codes) {
        this(typeReference, client, request, () -> {
        }, codes);
    }

    /**
     * Built by {@link HttpClientAdapter}, which owns the token the request carries.
     *
     * @param onRefusal runs when Consul answers {@code 403}, unless the caller listed 403 among the success codes
     */
    HttpInvocation(Class<R> type, HttpClient client, HttpRequest request, Runnable onRefusal, int... codes) {
        this(type, null, client, request, onRefusal, codes);
        Objects.requireNonNull(type, "type cannot be null");
    }

    /**
     * @param onRefusal runs when Consul answers {@code 403}, unless the caller listed 403 among the success codes
     */
    HttpInvocation(TypeReference<R> typeReference, HttpClient client, HttpRequest request, Runnable onRefusal, int... codes) {
        this(null, typeReference, client, request, onRefusal, codes);
        Objects.requireNonNull(typeReference, "typeReference cannot be null");
    }

    private HttpInvocation(Class<R> type, TypeReference<R> typeReference, HttpClient client, HttpRequest request, Runnable onRefusal, int... codes) {
        this.onRefusal = onRefusal;
        this.type = type;
        this.typeRef = typeReference;
        this.client = client;
        this.request = request;
        this.successCodes = codes.length == 0 ? Set.of(200) : IntStream.of(codes).boxed().collect(Collectors.toSet());
    }

    public <T> HttpInvocation<R> onError(ResponseHandler<T> errorHandler, int... codes) {
        Set<Integer> errorCodes = IntStream.of(codes).boxed().collect(Collectors.toSet());
        if (errorCodes.isEmpty()) {
            if (!errorHandlersMap.isEmpty()) {
                throw new IllegalArgumentException("codes cannot be empty when errorHandlersMap is already configured with: " + errorHandlersMap.keySet());
            } else {
                errorHandlersMap.put(ALL_ERRORS_CODE, errorHandler);
            }
        }
        errorCodes.forEach(code -> {
            if (errorHandlersMap.containsKey(code)) {
                throw new IllegalArgumentException("error code(s): " + errorCodes + " already specified: " + errorHandlersMap.keySet());
            } else if (code <= 0 || code >= 600) {
                throw new IllegalArgumentException("invalid code " + code);
            } else {
                errorHandlersMap.put(code, errorHandler);
            }
        });
        return this;
    }

    public R sendAndGet() throws InvocationException, ErrorCodeException {
        return this.send().getBody();
    }

    public ResponseHandler<R> send() throws InvocationException, ErrorCodeException {
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            int statusCode = response.statusCode();
            HttpHeaders headers = response.headers();
            String body = response.body();
            if (this.successCodes.stream().anyMatch(c -> c == statusCode)) {
                return this.type != null ?
                        new ResponseHandler<>(this.type).apply(body, headers) :
                        new ResponseHandler<>(this.typeRef).apply(body, headers);
            } else {
                if (statusCode == REFUSED) {
                    onRefusal.run();
                }
                ResponseHandler<String> defaultErrorConsumer = new ResponseHandler<>(String.class).apply(body, headers);
                ResponseHandler<?> errorHandler = this.errorHandlersMap.getOrDefault(statusCode, this.errorHandlersMap.get(ALL_ERRORS_CODE));
                if (errorHandler != null) {
                    errorHandler.apply(body, headers);
                    throw new ErrorCodeException(statusCode, errorHandler, defaultErrorConsumer);
                } else {
                    throw new DefaultErrorCodeException(statusCode, defaultErrorConsumer);
                }
            }
        } catch (IOException | InterruptedException e) {
            throw new InvocationException(e);
        }
    }


}

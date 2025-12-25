package com.netcracker.cloud.quarkus.consul.client.http;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.netcracker.cloud.quarkus.consul.client.model.GetValue;
import com.netcracker.cloud.security.core.utils.tls.TlsUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;

public class HttpTransport {
    private HttpClient httpClient;
    static final int DEFAULT_CONNECTION_TIMEOUT = 10 * 1000;
    Gson gson = new Gson();

    public HttpTransport() {
        this(HttpClient.newBuilder()
                .sslContext(TlsUtils.getSslContext())
                .connectTimeout(Duration.ofMillis(DEFAULT_CONNECTION_TIMEOUT))
                .executor(ForkJoinPool.commonPool())
                .build());
    }

    public HttpTransport(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public Response<List<GetValue>> makeGetRequest(String url, String... headers) {
        return makeGetRequestAsync(url, headers).join();
    }

    public CompletableFuture<Response<List<GetValue>>> makeGetRequestAsync(String url, String... headers) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET();
        if (headers.length > 0) builder.headers(headers);
        HttpRequest httpRequest = builder.build();
        CompletableFuture<HttpResponse<String>> future = httpClient.sendAsync(httpRequest, BodyHandlers.ofString());
        return future.thenApplyAsync(response -> {
            String content = response.body();
            Long consulIndex = parseUnsignedLong(response.headers().firstValue("X-Consul-Index"));
            Boolean consulKnownLeader = parseBoolean(response.headers().firstValue("X-Consul-Knownleader"));
            Long consulLastContact = parseUnsignedLong(response.headers().firstValue("X-Consul-Lastcontact"));
            if (response.statusCode() == 200) {
                List<GetValue> value = gson.fromJson(content, new TypeToken<List<GetValue>>() {
                }.getType());
                return new Response<>(value, consulIndex, consulKnownLeader, consulLastContact);
            } else if (response.statusCode() == 404) {
                return new Response<>(List.of(), consulIndex, consulKnownLeader, consulLastContact);
            } else {
                throw new OperationException(response.statusCode(), "An error occurred while executing the request", content);
            }
        });
    }

    private Long parseUnsignedLong(Optional<String> header) {
        return header.flatMap(value -> {
            try {
                return Optional.of(Long.parseUnsignedLong(value));
            } catch (Exception e) {
                return Optional.empty();
            }
        }).orElse(null);
    }

    private Boolean parseBoolean(Optional<String> header) {
        return header.map(value -> {
            if ("true".equals(value)) {
                return true;
            } else if ("false".equals(value)) {
                return false;
            }
            return null;
        }).orElse(null);
    }
}

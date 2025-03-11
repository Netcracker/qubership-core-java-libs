package org.qubership.cloud.quarkus.consul.client.http;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.qubership.cloud.quarkus.consul.client.model.GetValue;
import org.qubership.cloud.security.core.utils.tls.TlsUtils;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

public class HttpTransport {
    private HttpClient httpClient;
    static final int DEFAULT_CONNECTION_TIMEOUT = 10 * 1000;

    public HttpTransport(){
        this(HttpClient.newBuilder()
                .sslContext(TlsUtils.getSslContext())
                .connectTimeout(Duration.ofMillis(DEFAULT_CONNECTION_TIMEOUT))
                .build());
    }

    protected HttpTransport(HttpClient httpClient){
        this.httpClient = httpClient;
    }

    public Response<List<GetValue>> makeGetRequest(String url) {
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();
        HttpResponse<String> response = executeRequest(httpRequest);

        String content = response.body();
        Long consulIndex = parseUnsignedLong(response.headers().firstValue("X-Consul-Index"));
        Boolean consulKnownLeader = parseBoolean(response.headers().firstValue("X-Consul-Knownleader"));
        Long consulLastContact = parseUnsignedLong(response.headers().firstValue("X-Consul-Lastcontact"));

        if (response.statusCode() == 200) {
            Gson gson = new Gson();
            List<GetValue> value = gson.fromJson(content, new TypeToken<List<GetValue>>() {
            }.getType());
            return new Response<>(value, consulIndex, consulKnownLeader, consulLastContact);
        } else if (response.statusCode() == 404) {
            return new Response<>(null, consulIndex, consulKnownLeader, consulLastContact);
        } else {
            throw new OperationException(response.statusCode(), "An error occurred while executing the request", content);
        }
    }

    private HttpResponse<String> executeRequest(HttpRequest httpRequest) {
        try {
            return httpClient.send(httpRequest, BodyHandlers.ofString(StandardCharsets.UTF_8));

        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
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

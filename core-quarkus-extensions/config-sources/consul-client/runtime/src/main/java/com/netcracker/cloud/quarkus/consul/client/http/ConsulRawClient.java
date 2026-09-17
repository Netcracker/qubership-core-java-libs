package com.netcracker.cloud.quarkus.consul.client.http;

import com.netcracker.cloud.consul.provider.common.TokenStorage;
import com.netcracker.cloud.quarkus.consul.client.model.GetValue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

public class ConsulRawClient {

    private static final int REJECTED = 403;

    private final HttpTransport httpTransport;
    private final String agentAddress;
    private final TokenStorage tokenStorage;

    public ConsulRawClient(String consulUrl) {
        this(consulUrl, null);
    }

    /**
     * @param tokenStorage owner of the token, told through {@link TokenStorage#invalidate(String)} whenever Consul
     *                     answers {@code 403 ACL not found}. A client built without one leaves a refusal unreported
     */
    public ConsulRawClient(String consulUrl, TokenStorage tokenStorage) {
        this(new HttpTransport(), consulUrl, tokenStorage);
    }

    protected ConsulRawClient(HttpTransport httpTransport, String consulUrl) {
        this(httpTransport, consulUrl, null);
    }

    protected ConsulRawClient(HttpTransport httpTransport, String consulUrl, TokenStorage tokenStorage) {
        this.httpTransport = httpTransport;
        this.tokenStorage = tokenStorage;
        String consulUrlLowercase = consulUrl.toLowerCase();
        if (!consulUrlLowercase.startsWith("https://") && !consulUrlLowercase.startsWith("http://")) {
            consulUrlLowercase = "http://" + consulUrlLowercase;
        }
        this.agentAddress = consulUrlLowercase;
    }

    public Response<List<GetValue>> makeGetRequest(String endpoint, QueryParams queryParams, String token) {
        return makeGetRequestAsync(endpoint, queryParams, token).join();
    }

    /**
     * Every read of the Consul KV store passes through here, so this is where a token Consul no longer resolves is
     * reported. The returned future carries the same result and the same failure as before the report.
     */
    public CompletableFuture<Response<List<GetValue>>> makeGetRequestAsync(String endpoint, QueryParams queryParams, String token) {
        String url = generateUrl(agentAddress + endpoint, queryParams);
        return httpTransport.makeGetRequestAsync(url, "Authorization", String.format("Bearer %s", token))
                .whenComplete((response, failure) -> reportRejectedToken(failure, token));
    }

    private void reportRejectedToken(Throwable failure, String token) {
        if (tokenStorage == null) {
            return;
        }
        Throwable cause = failure;
        while ((cause instanceof CompletionException || cause instanceof ExecutionException) && cause.getCause() != null) {
            cause = cause.getCause();
        }
        if (cause instanceof OperationException && ((OperationException) cause).getStatusCode() == REJECTED) {
            tokenStorage.invalidate(token);
        }
    }

    public static String generateUrl(String baseUrl, QueryParams queryParams) {
        List<String> allParams = new ArrayList<>(queryParams.toUrlParameters());
        StringBuilder result = new StringBuilder(baseUrl);
        if (!allParams.isEmpty()) {
            result.append("?").append(String.join("&", allParams));
        }
        return result.toString();
    }

}

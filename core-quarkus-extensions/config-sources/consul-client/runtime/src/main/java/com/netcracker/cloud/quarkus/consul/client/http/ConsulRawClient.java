package com.netcracker.cloud.quarkus.consul.client.http;

import com.netcracker.cloud.quarkus.consul.client.model.GetValue;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

public class ConsulRawClient {

    private static final int REFUSED = 403;

    private static final Logger log = Logger.getLogger(ConsulRawClient.class);

    private final HttpTransport httpTransport;
    private final String agentAddress;
    private final Runnable onRefusal;

    public ConsulRawClient(String consulUrl) {
        this(consulUrl, () -> {
        });
    }

    /**
     * @param onRefusal runs whenever Consul answers {@code 403}, so that the owner of the token can check whether
     *                  Consul still resolves it
     */
    public ConsulRawClient(String consulUrl, Runnable onRefusal) {
        this(new HttpTransport(), consulUrl, onRefusal);
    }

    protected ConsulRawClient(HttpTransport httpTransport, String consulUrl) {
        this(httpTransport, consulUrl, () -> {
        });
    }

    protected ConsulRawClient(HttpTransport httpTransport, String consulUrl, Runnable onRefusal) {
        this.httpTransport = httpTransport;
        this.onRefusal = onRefusal;
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
                .whenComplete((response, failure) -> reportRefusal(failure));
    }

    private void reportRefusal(Throwable failure) {
        Throwable cause = failure;
        while ((cause instanceof CompletionException || cause instanceof ExecutionException) && cause.getCause() != null) {
            cause = cause.getCause();
        }
        if (cause instanceof OperationException && ((OperationException) cause).getStatusCode() == REFUSED) {
            log.debug("Consul refused the request; reporting it to the owner of the ACL token");
            onRefusal.run();
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

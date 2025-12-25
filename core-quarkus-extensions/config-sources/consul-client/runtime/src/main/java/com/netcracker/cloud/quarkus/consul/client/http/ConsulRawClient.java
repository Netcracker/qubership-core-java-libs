package com.netcracker.cloud.quarkus.consul.client.http;

import com.netcracker.cloud.quarkus.consul.client.model.GetValue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ConsulRawClient {

    private final HttpTransport httpTransport;
    private final String agentAddress;

    public ConsulRawClient(String consulUrl) {
        this(new HttpTransport(), consulUrl);
    }

    protected ConsulRawClient(HttpTransport httpTransport, String consulUrl) {
        this.httpTransport = httpTransport;
        String consulUrlLowercase = consulUrl.toLowerCase();
        if (!consulUrlLowercase.startsWith("https://") && !consulUrlLowercase.startsWith("http://")) {
            consulUrlLowercase = "http://" + consulUrlLowercase;
        }
        this.agentAddress = consulUrlLowercase;
    }

    public Response<List<GetValue>> makeGetRequest(String endpoint, QueryParams queryParams, String token) {
        return makeGetRequestAsync(endpoint, queryParams, token).join();
    }

    public CompletableFuture<Response<List<GetValue>>> makeGetRequestAsync(String endpoint, QueryParams queryParams, String token) {
        String url = generateUrl(agentAddress + endpoint, queryParams);
        return httpTransport.makeGetRequestAsync(url, "Authorization", String.format("Bearer %s", token));
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

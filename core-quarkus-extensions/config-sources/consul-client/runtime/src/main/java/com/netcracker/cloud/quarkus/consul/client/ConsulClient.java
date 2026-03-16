package com.netcracker.cloud.quarkus.consul.client;

import com.netcracker.cloud.quarkus.consul.client.http.ConsulRawClient;
import com.netcracker.cloud.quarkus.consul.client.http.QueryParams;
import com.netcracker.cloud.quarkus.consul.client.http.Response;
import com.netcracker.cloud.quarkus.consul.client.model.GetValue;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ConsulClient {

    private ConsulRawClient rawClient;

    public ConsulClient(ConsulRawClient rawClient) {
        this.rawClient = rawClient;
    }

    public ConsulClient(String consulUrl) {
        this(new ConsulRawClient(consulUrl));
    }

    public Response<List<GetValue>> getKVValues(String keyPrefix, String token) {
        return getKVValues(keyPrefix, token, new QueryParams(-1, -1));
    }

    public Response<List<GetValue>> getKVValues(String keyPrefix, String token, QueryParams queryParams) {
        return getKVValuesAsync(keyPrefix, token, queryParams).join();
    }

    public CompletableFuture<Response<List<GetValue>>> getKVValuesAsync(String keyPrefix, String token, QueryParams queryParams) {
        return rawClient.makeGetRequestAsync("/v1/kv/" + keyPrefix, queryParams, token);
    }
}

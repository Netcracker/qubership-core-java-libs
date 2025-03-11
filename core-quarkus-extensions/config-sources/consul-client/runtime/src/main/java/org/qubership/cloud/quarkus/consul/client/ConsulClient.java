package org.qubership.cloud.quarkus.consul.client;

import org.qubership.cloud.quarkus.consul.client.http.ConsulRawClient;
import org.qubership.cloud.quarkus.consul.client.model.GetValue;
import org.qubership.cloud.quarkus.consul.client.http.QueryParams;
import org.qubership.cloud.quarkus.consul.client.http.Response;

import java.util.List;

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
        queryParams.setToken(token);
        return rawClient.makeGetRequest("/v1/kv/" + keyPrefix, queryParams);
    }
}

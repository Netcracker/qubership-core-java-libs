package org.qubership.cloud.consul.provider.common.client;

import com.google.gson.Gson;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class ConsulOkHttpClient implements ConsulClient {

    private static final Logger log = LoggerFactory.getLogger(ConsulOkHttpClient.class);

    private final OkHttpClient client;
    private final String consulAddr;
    private final Supplier<String> m2mTokenSupplier;

    public ConsulOkHttpClient(OkHttpClient client, String consulAddr, Supplier<String> m2mTokenSupplier) {
        this.client = client;
        this.consulAddr = consulAddr;
        this.m2mTokenSupplier = m2mTokenSupplier;
    }

    @Override
    public ConsulClientResponse getSelfToken(String currentSecretId) {
        log.debug("Getting self token from {}", consulAddr);
        Response response;
        String responseBody = "";
        try {
            response = client.newCall(new Request.Builder().get()
                    .url(consulAddr + V1_ACL_TOKEN_SELF)
                    .addHeader(X_CONSUL_TOKEN_HEADER, currentSecretId)
                    .build()
            ).execute();
            responseBody = response.body().string();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return new ConsulClientResponse(responseBody, response.code());
    }

    @Override
    public ConsulClientResponse login(String authMethod) {
        Map<String, String> payload = new HashMap<>();
        payload.put(AUTH_METHOD_FIELD, authMethod);
        payload.put(BEARER_TOKEN_FIELD, m2mTokenSupplier.get());
        String json = new Gson().toJson(payload);
        log.info("Perform login to {} with {} auth method", consulAddr, authMethod);
        Response response;
        String responseBody = "";
        try {
            response = client.newCall(new Request.Builder()
                    .post(RequestBody.create(MediaType.parse(APPLICATION_JSON), json))
                    .url(consulAddr + V1_ACL_LOGIN)
                    .addHeader(CONTENT_TYPE, APPLICATION_JSON)
                    .build()
            ).execute();
            responseBody = response.body().string();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return new ConsulClientResponse(responseBody, response.code());
    }
}


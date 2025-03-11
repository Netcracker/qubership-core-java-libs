package org.qubership.cloud.quarkus.consul.client;

import org.qubership.cloud.consul.provider.common.TokenStorage;
import org.qubership.cloud.quarkus.consul.client.model.GetValue;
import org.qubership.cloud.quarkus.consul.client.http.QueryParams;
import org.qubership.cloud.quarkus.consul.client.http.Response;
import org.jboss.logging.Logger;

import java.util.List;

public class RetryableConsulClient {

    long SLEEP_ON_CONSUL_ERROR_PERIOD_MS = 10_000;

    private static final int MAX_CONSEQUENT_FAIL_AMOUNT = 3;

    private static final Logger log = Logger.getLogger(RetryableConsulClient.class);

    private final ConsulClient client;

    private final TokenStorage tokenStorage;

    public RetryableConsulClient(ConsulClient client, TokenStorage tokenStorage) {
        this.client = client;
        this.tokenStorage = tokenStorage;
    }

    public Response<List<GetValue>> getKVValues(String keyPrefix, QueryParams params) {
        Exception lastException = null;

        for (int i = 0; i < MAX_CONSEQUENT_FAIL_AMOUNT ; i++) {
            try {
                Response<List<GetValue>> resp = client.getKVValues(keyPrefix, tokenStorage.get(), params);
                return resp;
            } catch (RuntimeException e) {
                lastException = e;
                log.debug("Attempt {} failed: {}", i + 1, e);
            }
        }

        log.debug("Cannot get values from Consul, sleeping after 3 failed attempts", lastException);
        sleep(SLEEP_ON_CONSUL_ERROR_PERIOD_MS);
        return new Response<>(null, params.getIndex(), true, null);
    }

    private void sleep(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            throw new RuntimeException("Unexpected thread interrupt", e);
        }
    }

    public Response<List<GetValue>> callConsulWithRetry(String propertyRoot, Integer consulRetryTime){
        while (true) {
            try {
                return client.getKVValues(propertyRoot, tokenStorage.get());
            } catch (RuntimeException e) {
                log.debug("Cannot connect to consul with error message: " + e.getMessage());
                sleep(consulRetryTime);
            }
        }
    }
}

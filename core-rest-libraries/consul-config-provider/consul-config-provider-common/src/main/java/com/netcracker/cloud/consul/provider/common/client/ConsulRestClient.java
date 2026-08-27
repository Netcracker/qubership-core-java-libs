package com.netcracker.cloud.consul.provider.common.client;

import com.google.gson.Gson;
import com.netcracker.cloud.consul.provider.common.ConsulLoginCredentials;
import com.netcracker.cloud.restclient.HttpMethod;
import com.netcracker.cloud.restclient.MicroserviceRestClient;
import com.netcracker.cloud.restclient.entity.RestClientResponseEntity;
import com.netcracker.cloud.restclient.exception.MicroserviceRestClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/*
    @Deprecated.
    The usage of {@link ConsulRestClient} can cause issues.
    Use {@link com.netcracker.cloud.consul.provider.common.client.ConsulOkHttpClient} instead.
 */
@Deprecated
public class ConsulRestClient implements ConsulClient {

    private static final Logger log = LoggerFactory.getLogger(ConsulRestClient.class);

    private final MicroserviceRestClient client;
    private final String consulAddr;

    private final Supplier<String> m2mTokenSupplier;

    public ConsulRestClient(MicroserviceRestClient client, String consulAddr, Supplier<String> m2mTokenSupplier) {
        this.client = client;
        this.consulAddr = consulAddr;
        this.m2mTokenSupplier = m2mTokenSupplier;
    }

    @Override
    public ConsulClientResponse getSelfToken(String currentSecretId) {
        Map<String, List<String>> headers = new HashMap<>();
        headers.put(X_CONSUL_TOKEN_HEADER, Collections.singletonList(currentSecretId));

        log.debug("Getting self token from {}", consulAddr);
        RestClientResponseEntity<String> response = client.doRequest(consulAddr + V1_ACL_TOKEN_SELF, HttpMethod.GET, headers, null, String.class);
        return new ConsulClientResponse(response.getResponseBody(), response.getHttpStatus());
    }

    //todo vlla есть предложение удалить реализацию этого метода и бросить UnsupportedOperationException. Во первых, он deprecated, во вторых - после наших изменений код проходит только через метод login(ConsulLoginCredentials credentials) и нокогда через старый login (тут я могу ошибаться надо проверить).
    // нам это даст возможность не передавать m2mTokenSupplier в клиент и немного упростить com.netcracker.cloud.consul.provider.spring.common.config.ConsulM2MConfigDataLocationResolver.loadConfigProperties
    // но это только предложение, надо его проанализировать
    @Override
    public ConsulClientResponse login(String authMethod) {
        Map<String, String> payload = new HashMap<>();
        payload.put(AUTH_METHOD_FIELD, authMethod);
        payload.put(BEARER_TOKEN_FIELD, m2mTokenSupplier.get());
        String json = new Gson().toJson(payload);

        Map<String, List<String>> headers = new HashMap<>();
        headers.put(CONTENT_TYPE, Collections.singletonList(APPLICATION_JSON));

        log.debug("Perform login to {} with {} auth method", consulAddr, authMethod);
        RestClientResponseEntity<String> response = client.doRequest(consulAddr + V1_ACL_LOGIN, HttpMethod.POST, headers, json, String.class);
        return new ConsulClientResponse(response.getResponseBody(), response.getHttpStatus());
    }

    @Override
    public ConsulClientResponse login(ConsulLoginCredentials credentials) throws IOException {
        Map<String, String> payload = new HashMap<>();
        payload.put(AUTH_METHOD_FIELD, credentials.authMethod());
        payload.put(BEARER_TOKEN_FIELD, credentials.bearerToken());
        String json = new Gson().toJson(payload);

        Map<String, List<String>> headers = new HashMap<>();
        headers.put(CONTENT_TYPE, Collections.singletonList(APPLICATION_JSON));

        log.debug("Perform login to {} with {} auth method", consulAddr, credentials.authMethod());
        RestClientResponseEntity<String> response;
        try {
            response = client.doRequest(consulAddr + V1_ACL_LOGIN, HttpMethod.POST, headers, json, String.class);
        } catch (MicroserviceRestClientException e) {
            //todo vlla добавить комментарий, зачем здесь сделан выброс эксепшена
            throw new IOException("can not perform login to consul: " + e.getMessage(), e);
        }
        return new ConsulClientResponse(response.getResponseBody(), response.getHttpStatus());
    }
}

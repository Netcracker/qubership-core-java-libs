package org.qubership.cloud.consul.provider.common;

import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import org.qubership.cloud.consul.provider.common.client.ConsulClient;
import org.qubership.cloud.consul.provider.common.client.ConsulClientResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.OffsetDateTime;

public class TokenProvider {

    private static final Logger log = LoggerFactory.getLogger(TokenProvider.class);

    private final ConsulClient client;
    private final String authMethod;

    public TokenProvider(ConsulClient client, String authMethod) {
        this.client = client;
        this.authMethod = authMethod;
    }

    public Token getSelf(String currentSecretId) throws IOException {
        ConsulClientResponse response = client.getSelfToken(currentSecretId);
        String bodyJson = response.getBodyJson();
        if (response.getCode() != 200) {
            throw new IOException(String.format("can not get self token from consul; response code=%s; body='%s'", response.getCode(), bodyJson));
        }

        String secretId = JsonPath.read(bodyJson, "$.SecretID");
        OffsetDateTime expirationTime = OffsetDateTime.parse(JsonPath.read(bodyJson, "$.ExpirationTime"));
        log.debug("Got self token from Consul");
        return new Token(secretId, expirationTime);
    }

    public Token getNewConsulToken() throws IOException {
        ConsulClientResponse response = client.login(authMethod);
        String responseBody = response.getBodyJson();
        if (responseBody == null || responseBody.isEmpty()) {
            throw new IOException("can not get consul token by m2m token: response body is empty");
        }
        if (response.getCode() != 200) {
            throw new IOException("can not get consul token by m2m token: " + responseBody);
        }
        String secretId = JsonPath.read(responseBody, "$.SecretID");
        OffsetDateTime expirationTime = null;
        try {
            expirationTime = OffsetDateTime.parse(JsonPath.read(responseBody, "$.ExpirationTime"));
        } catch (PathNotFoundException ex) {
            // No Expiration Time. Nothing to do.
        }
        log.debug("Got new token from Consul by login procedure");
        return new Token(secretId, expirationTime);
    }
}

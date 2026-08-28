package com.netcracker.cloud.consul.provider.common;

import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import com.netcracker.cloud.consul.provider.common.client.ConsulClient;
import com.netcracker.cloud.consul.provider.common.client.ConsulClientResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.OffsetDateTime;

/**
 * Reads the expiration of an ACL token the pod already holds. The operation does not depend on how the token was
 * obtained, which is why it lives apart from {@link ConsulTokenProvider}.
 */
public class SelfTokenReader {

    private static final Logger log = LoggerFactory.getLogger(SelfTokenReader.class);

    private final ConsulClient client;

    public SelfTokenReader(ConsulClient client) {
        this.client = client;
    }

    /**
     * Reads the token behind {@code currentSecretId}. A token without an expiration is valid: Consul omits the field
     * for an auth method without {@code MaxTokenTTL}.
     *
     * @throws IOException on a non-2xx answer; the caller may retry
     * @throws RuntimeException on a transport failure, in whatever type the client throws. Unlike {@link
     *         ConsulClient#login(ConsulLoginCredentials)}, {@link ConsulClient#getSelfToken(String)} does not report
     *         it as an {@link IOException}, so the retry policies of the module do not cover it
     */
    public Token read(String currentSecretId) throws IOException {
        ConsulClientResponse response = client.getSelfToken(currentSecretId);
        String bodyJson = response.getBodyJson();
        if (response.getCode() != 200) {
            throw new IOException(String.format("can not get self token from consul; response code=%s; body='%s'", response.getCode(), bodyJson));
        }

        String secretId = JsonPath.read(bodyJson, "$.SecretID");
        OffsetDateTime expirationTime = null;
        try {
            expirationTime = OffsetDateTime.parse(JsonPath.read(bodyJson, "$.ExpirationTime"));
        } catch (PathNotFoundException ex) {
            // No Expiration Time. Nothing to do.
        }
        String authMethod = readAuthMethod(bodyJson);
        log.info("Got self token from Consul, issued by the {} auth method", authMethod == null ? "unknown" : authMethod);
        return new Token(secretId, expirationTime, authMethod);
    }

    private static String readAuthMethod(String bodyJson) {
        try {
            return JsonPath.read(bodyJson, "$.AuthMethod");
        } catch (PathNotFoundException ex) {
            return null;
        }
    }
}

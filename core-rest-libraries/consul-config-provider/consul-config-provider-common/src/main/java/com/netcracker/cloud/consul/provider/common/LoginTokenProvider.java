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
 * Obtains a token by performing a Consul login with one set of credentials. Knows nothing about which way the
 * credentials represent.
 */
public class LoginTokenProvider implements ConsulTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(LoginTokenProvider.class);

    private final ConsulClient client;
    private final ConsulLoginCredentials credentials;

    public LoginTokenProvider(ConsulClient client, ConsulLoginCredentials credentials) {
        this.client = client;
        this.credentials = credentials;
    }

    /**
     * @throws IOException on an empty body or a non-2xx answer; the message carries the response code, and a
     *         {@code 403} is reported as a Consul configuration that is not ready yet
     * @throws com.jayway.jsonpath.PathNotFoundException when the answer carries no {@code SecretID}; retrying that
     *         does not help
     */
    @Override
    public Token getToken() throws IOException {
        ConsulClientResponse response = client.login(credentials);
        String responseBody = response.getBodyJson();
        if (responseBody == null || responseBody.isEmpty()) {
            throw new IOException("can not get consul token by m2m token: response body is empty");
        }
        if (response.getCode() != 200) {
            String reason = response.getCode() == 403 ? "consul auth method is not ready" : "login to consul failed";
            throw new IOException(String.format("%s: response code=%s; body='%s'", reason, response.getCode(), responseBody));
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

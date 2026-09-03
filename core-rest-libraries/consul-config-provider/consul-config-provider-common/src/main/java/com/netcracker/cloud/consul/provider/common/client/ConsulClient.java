package com.netcracker.cloud.consul.provider.common.client;

import com.netcracker.cloud.consul.provider.common.ConsulLoginCredentials;

import java.io.IOException;

public interface ConsulClient {

    String V1_ACL_LOGIN = "/v1/acl/login";
    String V1_ACL_TOKEN_SELF = "/v1/acl/token/self";
    String X_CONSUL_TOKEN_HEADER = "X-Consul-Token";
    String AUTH_METHOD_FIELD = "AuthMethod";
    String BEARER_TOKEN_FIELD = "BearerToken";
    String APPLICATION_JSON = "application/json";
    String CONTENT_TYPE = "Content-Type";

    ConsulClientResponse getSelfToken(String currentSecretId);

    @Deprecated(forRemoval = true)
    ConsulClientResponse login(String authMethod);

    /**
     * Logs in with the given credentials. The default implementation delegates to the deprecated {@link
     * #login(String)}, so an implementation written before the credentials existed keeps working: it sends its own
     * bearer token and ignores the one in {@code credentials}.
     */
    default ConsulClientResponse login(ConsulLoginCredentials credentials) throws IOException {
        return login(credentials.getAuthMethod());
    }
}

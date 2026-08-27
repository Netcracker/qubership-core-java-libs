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

    default ConsulClientResponse login(ConsulLoginCredentials credentials) throws IOException {
        return login(credentials.getAuthMethod());
    }
}

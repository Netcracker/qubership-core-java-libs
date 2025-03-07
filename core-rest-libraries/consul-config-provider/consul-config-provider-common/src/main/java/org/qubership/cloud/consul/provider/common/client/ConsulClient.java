package org.qubership.cloud.consul.provider.common.client;

public interface ConsulClient {

    String V1_ACL_LOGIN = "/v1/acl/login";
    String V1_ACL_TOKEN_SELF = "/v1/acl/token/self";
    String X_CONSUL_TOKEN_HEADER = "X-Consul-Token";
    String AUTH_METHOD_FIELD = "AuthMethod";
    String BEARER_TOKEN_FIELD = "BearerToken";
    String APPLICATION_JSON = "application/json";
    String CONTENT_TYPE = "Content-Type";

    ConsulClientResponse getSelfToken(String currentSecretId);
    ConsulClientResponse login(String authMethod);
}

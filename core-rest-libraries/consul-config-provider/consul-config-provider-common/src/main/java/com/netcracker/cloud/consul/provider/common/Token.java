package com.netcracker.cloud.consul.provider.common;

import java.time.OffsetDateTime;

public class Token {
    private final String secretId;
    private final OffsetDateTime expirationTime;
    private final String authMethod;

    public Token(String secretId, OffsetDateTime expirationTime) {
        this(secretId, expirationTime, null);
    }

    public Token(String secretId, OffsetDateTime expirationTime, String authMethod) {
        this.secretId = secretId;
        this.expirationTime = expirationTime;
        this.authMethod = authMethod;
    }

    public String getSecretId() {
        return secretId;
    }

    public OffsetDateTime getExpirationTime() {
        return expirationTime;
    }

    /**
     * The auth method Consul issued the token to, or {@code null} when it is not known. Both a login answer and
     * {@code /v1/acl/token/self} carry the field, but only the self read parses it: a login already knows the way it
     * took.
     */
    public String getAuthMethod() {
        return authMethod;
    }
}

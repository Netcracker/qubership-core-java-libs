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
     * The auth method Consul issued the token to, or {@code null} when it is not known. A login answer does not carry
     * it; only {@code /v1/acl/token/self} does.
     */
    public String getAuthMethod() {
        return authMethod;
    }
}

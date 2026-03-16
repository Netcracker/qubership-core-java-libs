package com.netcracker.cloud.consul.provider.common;

import java.time.OffsetDateTime;

public class Token {
    private final String secretId;
    private final OffsetDateTime expirationTime;

    public Token(String secretId, OffsetDateTime expirationTime) {
        this.secretId = secretId;
        this.expirationTime = expirationTime;
    }

    public String getSecretId() {
        return secretId;
    }

    public OffsetDateTime getExpirationTime() {
        return expirationTime;
    }
}

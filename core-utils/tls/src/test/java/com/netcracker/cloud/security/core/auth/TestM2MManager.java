package com.netcracker.cloud.security.core.auth;

import java.time.Instant;

public class TestM2MManager implements M2MManager {

    @Override
    public Token getToken() {
        return new Token("Bearer", "test-token", Instant.now(), Instant.now().plusSeconds(60));
    }

    @Override
    public int priority() {
        return 10;
    }
}

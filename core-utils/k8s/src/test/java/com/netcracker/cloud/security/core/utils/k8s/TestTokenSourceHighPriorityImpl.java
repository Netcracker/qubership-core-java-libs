package com.netcracker.cloud.security.core.utils.k8s;

@Priority(1000)
public class TestTokenSourceHighPriorityImpl implements TokenSource {
    @Override
    public String getToken(String audience) {
        return "test-token";
    }

    @Override
    public void close() {
        // Test TokenSource: no resources to release.
    }
}


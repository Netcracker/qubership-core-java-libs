package com.netcracker.cloud.consul.provider.common;

/**
 * A {@link TokenStorage} that reports a rejected token to the {@link TokenUpdater} which owns it. Reads and writes go
 * to the wrapped storage untouched, so the stack that keeps the token — a Spring bean, a CDI singleton — stays the
 * one it was.
 */
public class SelfRefreshingTokenStorage implements TokenStorage {

    private final TokenStorage delegate;
    private final TokenUpdater tokenUpdater;

    public SelfRefreshingTokenStorage(TokenStorage delegate, TokenUpdater tokenUpdater) {
        this.delegate = delegate;
        this.tokenUpdater = tokenUpdater;
    }

    @Override
    public String get() {
        return delegate.get();
    }

    @Override
    public void update(String token) {
        delegate.update(token);
    }

    @Override
    public void invalidate(String rejectedToken) {
        tokenUpdater.invalidate(rejectedToken);
    }
}

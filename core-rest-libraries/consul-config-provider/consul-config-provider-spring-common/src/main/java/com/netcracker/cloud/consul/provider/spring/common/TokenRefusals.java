package com.netcracker.cloud.consul.provider.spring.common;

import com.netcracker.cloud.consul.provider.common.TokenStorage;

/**
 * Carries a refusal of the ACL token from the Consul client of the ConfigData phase to the {@link TokenStorage} that
 * owns the token.
 *
 * <p>The two are built in different phases: the client reads Consul before the application context exists, so the
 * storage arrives later and a refusal met before it arrives is dropped. The scheduled validation of the token updater
 * covers that window.
 *
 * <p>The ConfigData phase registers one instance and promotes it into the context, so the client and the bean that
 * subscribes hold the same object.
 */
public class TokenRefusals {

    private volatile TokenStorage tokenStorage;

    /**
     * Directs later refusals to {@code storage}. Called when the {@code TokenStorage} bean is built, which is the
     * first moment a refusal has an owner to go to.
     */
    public void reportTo(TokenStorage storage) {
        this.tokenStorage = storage;
    }

    /**
     * @param rejectedToken the token that was sent, which the storage compares with the one it holds now
     */
    public void report(String rejectedToken) {
        TokenStorage storage = tokenStorage;
        if (storage != null) {
            storage.invalidate(rejectedToken);
        }
    }
}

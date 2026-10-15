package com.netcracker.cloud.consul.provider.spring.common;

import com.netcracker.cloud.consul.provider.common.ConsulTokenSource;

/**
 * Carries a refusal from the Consul client of the ConfigData phase to the source that owns the ACL token.
 *
 * <p>The two are built in different phases: the client reads Consul before the application context exists, so the
 * source arrives later and a refusal met before it arrives is dropped. The scheduled check of the token covers that
 * window.
 *
 * <p>The ConfigData phase registers one instance and promotes it into the context, so the client and the bean that
 * subscribes hold the same object.
 */
public class TokenRefusals {

    private volatile ConsulTokenSource tokenSource;

    /**
     * Directs later refusals to {@code source}. Called when the token source is built, which is the first moment a
     * refusal has an owner to go to.
     */
    public void reportTo(ConsulTokenSource source) {
        this.tokenSource = source;
    }

    public void report() {
        ConsulTokenSource source = tokenSource;
        if (source != null) {
            source.reportRefusal();
        }
    }
}

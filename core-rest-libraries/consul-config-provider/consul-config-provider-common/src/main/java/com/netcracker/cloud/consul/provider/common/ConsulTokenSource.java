package com.netcracker.cloud.consul.provider.common;

/**
 * The Consul ACL token of the pod, together with the channel a consumer reports a refusal on.
 *
 * <p>A consumer sends {@link #get()} with every request and calls {@link #reportRefusal()} when Consul answers
 * {@code 403}. Deciding what that answer means is not the consumer's job: Consul returns the same code for a token it
 * no longer resolves and for a live token whose policy is short of a privilege, and only the first calls for a new
 * token.
 */
public interface ConsulTokenSource {

    /**
     * Returns the token to send, or an empty string before the first login completes.
     */
    String get();

    /**
     * Reports that Consul answered {@code 403} to a request that carried this token.
     *
     * <p>The source reads the token back from Consul and logs in again only when Consul no longer resolves it. A
     * report is therefore safe to make on every refusal, including a refusal of a token already replaced and a refusal
     * caused by a missing privilege.
     *
     * <p>Returns at once and throws nothing: the check and the login run on the executor of the source. However many
     * consumers report at the same time, at most one login follows, and no sooner than a minimum interval after the
     * previous check.
     */
    void reportRefusal();
}

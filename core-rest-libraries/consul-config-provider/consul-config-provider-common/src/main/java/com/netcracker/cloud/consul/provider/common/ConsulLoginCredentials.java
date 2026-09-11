package com.netcracker.cloud.consul.provider.common;

/**
 * The pair Consul expects in a login request: the auth method to log in to, and the bearer token proving the identity.
 */
public interface ConsulLoginCredentials {

    String getAuthMethod();

    /**
     * Reads the bearer token from its source. Called before every login attempt, so the source stays free to rotate
     * the token; implementations cache nothing of their own.
     */
    String getBearerToken();
}

package com.netcracker.cloud.consul.provider.common;

import java.io.IOException;

/**
 * Obtains a fresh Consul ACL token. Implementations either perform one login or choose between several ways of
 * performing it.
 */
public interface ConsulTokenProvider {

    /**
     * Performs a Consul login and returns the issued token.
     *
     * @throws IOException on a transport failure or a non-2xx answer from Consul; the caller may retry
     */
    Token getToken() throws IOException;

    /**
     * Reads the token the pod already holds instead of obtaining a new one. Consul reports the auth method the token
     * was issued to, so a provider that carries state across logins recovers it here rather than starting over.
     *
     * @throws IOException on a non-2xx answer or an empty body; the caller may retry. Unlike {@link #getToken()}, this
     *         read reports a transport failure unchecked, in whatever type the client throws
     */
    Token getSelfToken(String currentSecretId) throws IOException;
}

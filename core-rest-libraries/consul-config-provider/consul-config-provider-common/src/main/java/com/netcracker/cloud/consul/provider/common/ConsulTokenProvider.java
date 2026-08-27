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
}

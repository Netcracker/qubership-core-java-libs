package com.netcracker.cloud.consul.provider.common;


public interface TokenStorage {
    String get();

    void update(String token);

    /**
     * Reports that Consul refused {@code rejectedToken} with {@code 403 ACL not found}, so that the owner of the token
     * obtains a new one. The caller passes the token it sent rather than the one the storage holds now: a signal about
     * a token already replaced is dropped instead of forcing one more login.
     *
     * <p>Report only a token Consul refused to resolve. A login refused with the same code means the auth method
     * rejected the bearer, and forcing a relogin on it loops.
     *
     * <p>Does nothing unless the storage owns a token updater. Storage implementations of this module are plain
     * holders and keep the default; {@link TokenStorageFactory#create(TokenStorageFactory.CreateOptions)} wraps one
     * into a storage that acts on the signal.
     */
    default void invalidate(String rejectedToken) {
    }
}

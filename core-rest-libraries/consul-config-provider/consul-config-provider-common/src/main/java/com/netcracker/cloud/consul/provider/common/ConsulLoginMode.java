package com.netcracker.cloud.consul.provider.common;

/**
 * Which way of obtaining a Consul ACL token a pod uses. The mode is read at startup and does not change, but in the
 * {@link #HYBRID} mode the way the pod actually takes can change while it runs.
 */
public enum ConsulLoginMode {
    /** Only the m2m way. The auth method name and the audience are not used. */
    LEGACY,
    /** Tries the kubernetes way, serves m2m while that fails, and retries it on the scheduled relogin. */
    HYBRID,
    /** Only the kubernetes way, with no probe and no fallback: a failed login is not retried by another way. */
    K8S
}

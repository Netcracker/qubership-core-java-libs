package com.netcracker.cloud.consul.provider.common;

/**
 * Which way of obtaining a Consul ACL token a pod uses. The mode is read at startup and does not change, but in the
 * fallback mode the way the pod actually takes can change while it runs.
 */
public enum ConsulLoginMode {
    /** Tries the kubernetes way, serves m2m while that fails, and retries it on the scheduled relogin. */
    KUBERNETES_WITH_M2M_FALLBACK,
    /** Only the kubernetes way, with no probe and no fallback: a failed login is not retried by another way. */
    KUBERNETES,
    /** Only the m2m way. The auth method name and the audience are not used. */
    M2M
}

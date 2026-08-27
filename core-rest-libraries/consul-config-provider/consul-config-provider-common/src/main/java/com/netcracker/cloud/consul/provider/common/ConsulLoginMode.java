package com.netcracker.cloud.consul.provider.common;

/**
 * Which way of obtaining a Consul ACL token a pod uses. A pod keeps one way for its whole life.
 */
public enum ConsulLoginMode {
    /** Tries the kubernetes way once, and falls back to m2m for the rest of the pod's life if that fails. */
    KUBERNETES_WITH_M2M_FALLBACK,
    /** Only the kubernetes way, with no probe and no fallback. A failed login is a failure. */
    KUBERNETES,
    /** Only the m2m way. The auth method name and the audience are not read. */
    M2M
}

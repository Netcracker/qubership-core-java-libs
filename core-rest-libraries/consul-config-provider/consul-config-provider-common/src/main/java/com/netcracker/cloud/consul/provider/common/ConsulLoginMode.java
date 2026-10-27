package com.netcracker.cloud.consul.provider.common;

import com.netcracker.cloud.security.core.utils.k8s.M2MAuthMode;

import java.util.function.Supplier;

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
    K8S;

    /**
     * The mode a pod logs in with: {@code configured} when it is given, otherwise the mode {@code M2M_AUTH_MODE} names.
     * One variable thus sets the M2M calls and the Consul login together, and {@code consul.auth.mode} overrides it
     * for the Consul login alone. An unset or blank {@code M2M_AUTH_MODE} is {@link #LEGACY}.
     *
     * @throws IllegalArgumentException if {@code configured} is {@code null} and {@code M2M_AUTH_MODE} is not legacy,
     *                                  hybrid or k8s
     */
    public static ConsulLoginMode resolve(ConsulLoginMode configured) {
        return resolve(configured, M2MAuthMode::readFromEnv);
    }

    /**
     * {@link #resolve(ConsulLoginMode)} with the M2M mode taken from {@code m2mAuthMode}, which is asked only when
     * {@code configured} is {@code null}.
     */
    public static ConsulLoginMode resolve(ConsulLoginMode configured, Supplier<M2MAuthMode> m2mAuthMode) {
        if (configured != null) {
            return configured;
        }
        M2MAuthMode m2m;
        try {
            m2m = m2mAuthMode.get();
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("consul.auth.mode is not set, so the consul login mode is taken from "
                    + M2MAuthMode.M2M_AUTH_MODE_ENV + ": " + e.getMessage(), e);
        }
        return switch (m2m) {
            case LEGACY -> LEGACY;
            case HYBRID -> HYBRID;
            case K8S -> K8S;
        };
    }
}

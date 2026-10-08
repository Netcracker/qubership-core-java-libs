package com.netcracker.cloud.security.core.utils.k8s;

import java.util.Locale;

public enum M2MAuthMode {
    /**
     * Sends the legacy M2M token; DBaaS and MaaS requests go through their agents.
     */
    LEGACY,
    /**
     * Sends the Kubernetes token and falls back to the legacy M2M token when the Kubernetes token cannot be read or
     * the receiver answers 401.
     */
    HYBRID,
    /**
     * Sends only the Kubernetes token, without a fallback.
     */
    K8S;

    public static final String M2M_AUTH_MODE_ENV = "M2M_AUTH_MODE";

    public static M2MAuthMode readFromEnv() {
        return parse(System.getenv(M2M_AUTH_MODE_ENV));
    }

    /**
     * Matches {@code value} case-insensitively with surrounding whitespace ignored. A {@code null} or blank value is
     * {@link #LEGACY}.
     *
     * @throws IllegalArgumentException if {@code value} is not legacy, hybrid, or k8s
     */
    static M2MAuthMode parse(String value) {
        if (value == null || value.isBlank()) {
            return LEGACY;
        }
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "legacy" -> LEGACY;
            case "hybrid" -> HYBRID;
            case "k8s" -> K8S;
            default -> throw new IllegalArgumentException(
                    M2M_AUTH_MODE_ENV + " has unsupported value \"" + value + "\": set it to legacy, hybrid, or k8s");
        };
    }
}

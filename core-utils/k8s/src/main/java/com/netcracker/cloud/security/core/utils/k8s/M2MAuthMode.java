package com.netcracker.cloud.security.core.utils.k8s;

import java.util.Locale;

public enum M2MAuthMode {
    LEGACY,
    /**
     * Sends the Kubernetes token and falls back to the legacy M2M token when the Kubernetes token cannot be read or
     * the receiver rejects it.
     */
    HYBRID,
    K8S;

    public static final String ENV = "M2M_AUTH_MODE";

    public static M2MAuthMode read() {
        return parse(System.getenv(ENV));
    }

    /**
     * Matches {@code value} case-insensitively with surrounding whitespace ignored. A {@code null} or blank value is
     * {@link #LEGACY}.
     *
     * @throws IllegalArgumentException if {@code value} is not legacy, hybrid, or k8s
     */
    public static M2MAuthMode parse(String value) {
        if (value == null || value.isBlank()) {
            return LEGACY;
        }
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "legacy" -> LEGACY;
            case "hybrid" -> HYBRID;
            case "k8s" -> K8S;
            default -> throw new IllegalArgumentException(
                    ENV + " has unsupported value \"" + value + "\": set it to legacy, hybrid, or k8s");
        };
    }
}

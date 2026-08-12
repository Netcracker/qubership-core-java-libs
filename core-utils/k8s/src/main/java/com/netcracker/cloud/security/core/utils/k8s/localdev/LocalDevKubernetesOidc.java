package com.netcracker.cloud.security.core.utils.k8s.localdev;

import com.netcracker.cloud.security.core.utils.k8s.localdev.impl.KubeLocalDevConfig;
import org.jetbrains.annotations.VisibleForTesting;

/**
 * Public facade for local-dev Kubernetes OIDC helpers used by security consumers
 * (JWKS URL rewrite, issuer discovery).
 */
public final class LocalDevKubernetesOidc {

    private static volatile KubeLocalDevConfig config = new KubeLocalDevConfig();

    private LocalDevKubernetesOidc() {
    }

    public static boolean isKubernetesIssuer(String issuerOrUrl) {
        return config.isKubernetesIssuer(issuerOrUrl);
    }

    public static String jwksUrl() {
        return config.jwksUrl();
    }

    /**
     * Resolves the Kubernetes token issuer claim from OIDC discovery when a projected SA token is unavailable.
     */
    public static String resolveIssuerClaimFromDiscovery() {
        return config.resolveIssuerClaimFromDiscovery();
    }

    @VisibleForTesting
    public static void resetCache() {
        config = new KubeLocalDevConfig();
    }
}

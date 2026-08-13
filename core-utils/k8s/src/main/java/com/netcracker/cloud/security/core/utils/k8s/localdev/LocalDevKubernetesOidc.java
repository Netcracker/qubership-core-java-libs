package com.netcracker.cloud.security.core.utils.k8s.localdev;

import com.netcracker.cloud.security.core.utils.k8s.localdev.impl.KubeLocalDevConfig;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Public facade for local-dev Kubernetes OIDC helpers used by security consumers
 * (JWKS URL rewrite, issuer discovery).
 */
public final class LocalDevKubernetesOidc {

    private static final AtomicReference<KubeLocalDevConfig> config =
            new AtomicReference<>(new KubeLocalDevConfig());

    private LocalDevKubernetesOidc() {
    }

    public static boolean isKubernetesIssuer(String issuerOrUrl) {
        return config.get().isKubernetesIssuer(issuerOrUrl);
    }

    public static String jwksUrl() {
        return config.get().jwksUrl();
    }

    /**
     * Resolves the Kubernetes token issuer claim from OIDC discovery when a projected SA token is unavailable.
     */
    public static String resolveIssuerClaimFromDiscovery() {
        return config.get().resolveIssuerClaimFromDiscovery();
    }

    @VisibleForTesting
    public static void resetCache() {
        config.set(new KubeLocalDevConfig());
    }
}

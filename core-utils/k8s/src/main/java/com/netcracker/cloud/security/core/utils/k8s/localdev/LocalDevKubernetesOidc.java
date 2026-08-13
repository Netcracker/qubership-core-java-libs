package com.netcracker.cloud.security.core.utils.k8s.localdev;

import com.netcracker.cloud.security.core.utils.k8s.localdev.impl.KubeLocalDevConfig;

/**
 * Local-dev Kubernetes OIDC helpers used by security consumers
 * (JWKS URL rewrite, issuer discovery).
 */
public final class LocalDevKubernetesOidc {

    private final KubeLocalDevConfig config;

    public LocalDevKubernetesOidc() {
        this.config = new KubeLocalDevConfig();
    }

    public boolean isKubernetesIssuer(String issuerOrUrl) {
        return config.isKubernetesIssuer(issuerOrUrl);
    }

    public String jwksUrl() {
        return config.jwksUrl();
    }

    /**
     * Resolves the Kubernetes token issuer claim from OIDC discovery when a projected SA token is unavailable.
     */
    public String resolveIssuerClaimFromDiscovery() {
        return config.resolveIssuerClaimFromDiscovery();
    }
}

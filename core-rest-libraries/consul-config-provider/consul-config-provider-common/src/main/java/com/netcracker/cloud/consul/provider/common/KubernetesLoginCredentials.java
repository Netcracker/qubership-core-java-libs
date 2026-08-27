package com.netcracker.cloud.consul.provider.common;

import com.netcracker.cloud.security.core.utils.k8s.KubernetesAudienceToken;

/**
 * Credentials of the kubernetes way: the bearer token is the projected service account token of the pod, read by
 * audience through the {@code TokenSource} SPI.
 */
final class KubernetesLoginCredentials implements ConsulLoginCredentials {

    private final String authMethod;
    private final String audience;

    KubernetesLoginCredentials(String authMethod, String audience) {
        this.authMethod = authMethod;
        this.audience = audience;
    }

    @Override
    public String getAuthMethod() {
        return authMethod;
    }

    @Override
    public String getBearerToken() {
        return KubernetesAudienceToken.getToken(audience);
    }
}

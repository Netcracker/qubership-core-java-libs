package com.netcracker.cloud.consul.provider.common;

import com.netcracker.cloud.security.core.utils.k8s.KubernetesAudienceToken;

final class KubernetesLoginCredentials implements ConsulLoginCredentials {

    private final String authMethod;
    private final String audience;

    KubernetesLoginCredentials(String authMethod, String audience) {
        this.authMethod = authMethod;
        this.audience = audience;
    }

    @Override
    public String authMethod() {
        return authMethod;
    }

    @Override
    public String bearerToken() {
        return KubernetesAudienceToken.getToken(audience);
    }
}

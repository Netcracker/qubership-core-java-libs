package com.netcracker.cloud.security.core.utils.k8s;

import com.netcracker.cloud.security.core.utils.k8s.impl.M2MInterceptor;
import com.netcracker.cloud.security.core.utils.k8s.impl.UrlCache;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import okhttp3.OkHttpClient;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Builds okhttp clients for m2m communication:
 * <pre>{@code
 * OkHttpClient client = M2MClient.builder()
 *         .audience(AudienceName.DBAAS)
 *         .agentUrl(dbaasAgentUrl)
 *         .keycloakTokenSupplier(() -> m2mManager.getToken().getTokenValue())
 *         .build();
 * }</pre>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class M2MClient {
    public static boolean isK8sM2mEnabled() {
        return Boolean.parseBoolean(System.getenv("KUBERNETES_M2M_ENABLED"));
    }

    public static M2MClientBuilder builder() {
        return new M2MClientBuilder();
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class M2MClientBuilder {
        private String audience = AudienceName.NETCRACKER;
        private String agentUrl;
        private Supplier<String> keycloakTokenSupplier;
        private boolean k8sM2mEnabled = isK8sM2mEnabled();

        public M2MClientBuilder audience(String audience) {
            this.audience = Objects.requireNonNull(audience, "audience must not be null");
            return this;
        }

        public M2MClientBuilder agentUrl(String agentUrl) {
            this.agentUrl = agentUrl;
            return this;
        }

        public M2MClientBuilder keycloakTokenSupplier(Supplier<String> keycloakTokenSupplier) {
            this.keycloakTokenSupplier = keycloakTokenSupplier;
            return this;
        }

        public M2MClientBuilder k8sM2mEnabled(boolean k8sM2mEnabled) {
            this.k8sM2mEnabled = k8sM2mEnabled;
            return this;
        }

        public OkHttpClient build() {
            Objects.requireNonNull(keycloakTokenSupplier, "keycloakTokenSupplier must be set");
            M2MInterceptor interceptor = new M2MInterceptor(
                    k8sM2mEnabled,
                    new UrlCache(),
                    bearerAuthHeaderSupplier(keycloakTokenSupplier),
                    bearerAuthHeaderSupplier(() -> KubernetesAudienceToken.getToken(audience)),
                    agentUrl);
            return new OkHttpClient.Builder()
                    .addInterceptor(interceptor)
                    .build();
        }

        private static Supplier<String> bearerAuthHeaderSupplier(Supplier<String> tokenSupplier) {
            return () -> "Bearer " + tokenSupplier.get();
        }
    }
}

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
    public static M2MClientBuilder builder() {
        return new M2MClientBuilder();
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class M2MClientBuilder {
        private String audience = AudienceName.NETCRACKER;
        private String agentUrl;
        private Supplier<String> keycloakTokenSupplier;
        private M2MAuthMode mode = M2MAuthMode.readFromEnv();

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

        public M2MClientBuilder mode(M2MAuthMode mode) {
            this.mode = Objects.requireNonNull(mode, "mode must not be null");
            return this;
        }

        public OkHttpClient build() {
            if (mode != M2MAuthMode.K8S && keycloakTokenSupplier == null) {
                throw new IllegalStateException("keycloakTokenSupplier must be set unless the M2M auth mode is k8s");
            }
            M2MInterceptor interceptor = new M2MInterceptor(
                    mode,
                    new UrlCache(),
                    keycloakTokenSupplier == null ? null : bearerAuthHeaderSupplier(keycloakTokenSupplier),
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

package com.netcracker.cloud.dbaas.client;

import com.netcracker.cloud.dbaas.client.config.DbaasApiAddress;
import com.netcracker.cloud.security.core.auth.M2MManager;
import com.netcracker.cloud.security.core.utils.k8s.AudienceName;
import com.netcracker.cloud.security.core.utils.k8s.M2MAuthMode;
import com.netcracker.cloud.security.core.utils.k8s.M2MClient;
import com.netcracker.cloud.security.core.utils.tls.TlsUtils;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;

import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/**
 * Builds a {@link DbaasClient} that authorizes its own requests, as {@code M2M_AUTH_MODE} tells it to.
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DbaasClientFactory {

    public static DbaasClient newDbaasClient() {
        return builder().build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String dbaasAddress;
        private String agentAddress;
        private M2MAuthMode mode;
        private Supplier<String> legacyTokenSupplier;
        private UnaryOperator<OkHttpClient.Builder> httpClientBuilder;

        private Builder() {
        }

        /**
         * DBaaS API address instead of {@value DbaasApiAddress#DBAAS_ADDRESS_PROPERTY}, used as soon as requests carry
         * a kubernetes token.
         */
        public Builder dbaasAddress(String dbaasAddress) {
            this.dbaasAddress = dbaasAddress;
            return this;
        }

        /**
         * dbaas-agent address used by legacy tokens, {@value DbaasApiAddress#AGENT_ADDRESS_PROPERTY} by default.
         */
        public Builder agentAddress(String agentAddress) {
            this.agentAddress = agentAddress;
            return this;
        }

        /**
         * Authentication mode, {@code M2M_AUTH_MODE} by default.
         */
        public Builder mode(M2MAuthMode mode) {
            this.mode = mode;
            return this;
        }

        /**
         * Legacy token supplier, read only when the mode is not k8s. An {@link M2MManager} service is used when unset.
         */
        public Builder legacyToken(Supplier<String> legacyTokenSupplier) {
            this.legacyTokenSupplier = legacyTokenSupplier;
            return this;
        }

        /**
         * Applies own interceptors and transport settings on top of the ones this factory provides.
         */
        public Builder httpClient(UnaryOperator<OkHttpClient.Builder> httpClientBuilder) {
            this.httpClientBuilder = httpClientBuilder;
            return this;
        }

        public DbaasClient build() {
            M2MAuthMode authMode = mode != null ? mode : M2MAuthMode.readFromEnv();
            DbaasApiAddress address = DbaasApiAddress.resolve(authMode, agentAddress, dbaasAddress);

            OkHttpClient httpClient = M2MClient.builder()
                    .audience(AudienceName.DBAAS)
                    .agentUrl(address.agentUrl())
                    .mode(authMode)
                    .keycloakTokenSupplier(legacyToken(authMode))
                    .build()
                    .newBuilder()
                    .addInterceptor(HttpErrorRetrier.forProvisioning())
                    .sslSocketFactory(TlsUtils.getSslContext().getSocketFactory(), TlsUtils.getTrustManager())
                    .build();
            if (httpClientBuilder != null) {
                httpClient = httpClientBuilder.apply(httpClient.newBuilder()).build();
            }

            log.info("DBaaS client sends requests to {} with {} authentication", address.url(), authMode);
            return new DbaaSClientOkHttpImpl(address.url(), httpClient);
        }

        private Supplier<String> legacyToken(M2MAuthMode authMode) {
            if (legacyTokenSupplier != null) {
                return legacyTokenSupplier;
            }
            if (authMode == M2MAuthMode.K8S) {
                return null;
            }
            M2MManager manager = M2MManager.loadPreferred();
            return () -> manager.getToken().getTokenValue();
        }
    }
}

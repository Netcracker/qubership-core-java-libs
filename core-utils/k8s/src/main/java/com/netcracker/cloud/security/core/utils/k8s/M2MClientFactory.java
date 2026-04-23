package com.netcracker.cloud.security.core.utils.k8s;

import com.netcracker.cloud.security.core.utils.k8s.impl.M2MInterceptor;
import com.netcracker.cloud.security.core.utils.k8s.impl.UrlCache;
import com.netcracker.cloud.security.core.utils.tls.TlsUtils;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;

import java.util.Base64;
import java.util.function.Supplier;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class M2MClientFactory {
    public static OkHttpClient getM2MClient(Supplier<String> keycloakTokenSupplier) {
        return getClient(() -> getBearerAuthHeader(keycloakTokenSupplier));
    }

    public static OkHttpClient getDBaaSClient(Supplier<String> keycloakTokenSupplier) {
        return getClient(() -> getBearerAuthHeader(keycloakTokenSupplier));
    }

    public static OkHttpClient getMaaSClient(Supplier<String> keycloakTokenSupplier) {
        return getClient(() -> getBearerAuthHeader(keycloakTokenSupplier));
    }

    private static OkHttpClient getClient(Supplier<String> fallbackAuthHeaderSupplier) {
        final OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .sslSocketFactory(TlsUtils.getSslContext().getSocketFactory(), TlsUtils.getTrustManager());
        final M2MInterceptor interceptor = new M2MInterceptor(
                new UrlCache(),
                fallbackAuthHeaderSupplier,
                () -> getBearerAuthHeader(() -> KubernetesAudienceToken.getToken(AudienceName.NETCRACKER))
        );
        builder.addInterceptor(interceptor);
        return builder.build();
    }

    private static String getBearerAuthHeader(Supplier<String> tokenSupplier) {
        try {
            return "Bearer "+tokenSupplier.get();
        } catch (IllegalArgumentException ex) {
            log.error("Error during kubernetes m2m token acquisition", ex);
            return null;
        }
    }
}

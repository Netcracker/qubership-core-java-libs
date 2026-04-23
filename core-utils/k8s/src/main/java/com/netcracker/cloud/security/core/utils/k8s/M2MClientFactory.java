package com.netcracker.cloud.security.core.utils.k8s;

import com.netcracker.cloud.security.core.utils.k8s.impl.M2MInterceptor;
import com.netcracker.cloud.security.core.utils.k8s.impl.UrlCache;
import com.netcracker.cloud.security.core.utils.tls.TlsUtils;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;

import java.util.function.Supplier;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class M2MClientFactory {

    private static final Supplier<String> k8sAuthHeaderSupplier =
            getBearerAuthHeaderSupplier(() -> KubernetesAudienceToken.getToken(AudienceName.NETCRACKER));

    public static OkHttpClient getM2MClient(Supplier<String> keycloakTokenSupplier) {
        return getClient(new M2MInterceptor(new UrlCache(), getBearerAuthHeaderSupplier(keycloakTokenSupplier), k8sAuthHeaderSupplier));
    }

    public static OkHttpClient getDBaaSClient(Supplier<String> keycloakTokenSupplier) {
        return getAgentClient(keycloakTokenSupplier, "http://dbaas-agent:8080");
    }

    public static OkHttpClient getMaaSClient(Supplier<String> keycloakTokenSupplier) {
        return getAgentClient(keycloakTokenSupplier, "http://maas-agent:8080");
    }

    private static OkHttpClient getAgentClient(Supplier<String> keycloakTokenSupplier, String agentUrl) {
        return getClient(new M2MInterceptor(new UrlCache(), getBearerAuthHeaderSupplier(keycloakTokenSupplier), k8sAuthHeaderSupplier, agentUrl));
    }

    private static OkHttpClient getClient(M2MInterceptor interceptor) {
        return new OkHttpClient.Builder()
                .sslSocketFactory(TlsUtils.getSslContext().getSocketFactory(), TlsUtils.getTrustManager())
                .addInterceptor(interceptor)
                .build();
    }

    private static Supplier<String> getBearerAuthHeaderSupplier(Supplier<String> tokenSupplier) {
        return () -> "Bearer " + tokenSupplier.get();
    }
}

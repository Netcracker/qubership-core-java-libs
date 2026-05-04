package com.netcracker.cloud.security.core.utils.k8s;

import com.netcracker.cloud.security.core.utils.k8s.impl.M2MInterceptor;
import com.netcracker.cloud.security.core.utils.k8s.impl.UrlCache;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;

import java.util.Optional;
import java.util.function.Supplier;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class M2MClientFactory {
    public static final String DBAAS_AGENT_URL_PROP = "com.netcracker.cloud.dbaas.agent.url";
    public static final String MAAS_AGENT_URL_PROP = "com.netcracker.cloud.maas.agent.url";

    private static final Supplier<String> k8sAuthHeaderSupplier =
            getBearerAuthHeaderSupplier(() -> KubernetesAudienceToken.getToken(AudienceName.NETCRACKER));

    public static OkHttpClient getM2MClient(Supplier<String> keycloakTokenSupplier) {
        return getClient(new M2MInterceptor(new UrlCache(), getBearerAuthHeaderSupplier(keycloakTokenSupplier), k8sAuthHeaderSupplier));
    }

    public static OkHttpClient getDBaaSClient(Supplier<String> keycloakTokenSupplier) {
        return getAgentClient(keycloakTokenSupplier, Optional.ofNullable(System.getProperty(DBAAS_AGENT_URL_PROP)).orElse("http://dbaas-agent:8080"));
    }

    public static OkHttpClient getMaaSClient(Supplier<String> keycloakTokenSupplier) {
        return getAgentClient(keycloakTokenSupplier, Optional.ofNullable(System.getProperty(MAAS_AGENT_URL_PROP)).orElse("http://maas-agent:8080"));
    }

    private static OkHttpClient getAgentClient(Supplier<String> keycloakTokenSupplier, String agentUrl) {
        return getClient(new M2MInterceptor(new UrlCache(), getBearerAuthHeaderSupplier(keycloakTokenSupplier), k8sAuthHeaderSupplier, agentUrl));
    }

    private static OkHttpClient getClient(M2MInterceptor interceptor) {
        return new OkHttpClient.Builder()
                .addInterceptor(interceptor)
                .build();
    }

    private static Supplier<String> getBearerAuthHeaderSupplier(Supplier<String> tokenSupplier) {
        return () -> "Bearer " + tokenSupplier.get();
    }
}

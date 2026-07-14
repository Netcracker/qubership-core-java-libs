package com.netcracker.cloud.security.core.utils.k8s;

import com.netcracker.cloud.context.propagation.core.RequestContextPropagation;
import com.netcracker.cloud.security.core.utils.k8s.impl.M2MInterceptor;
import com.netcracker.cloud.security.core.utils.k8s.impl.UrlCache;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import okhttp3.OkHttpClient;

import java.util.Optional;
import java.util.function.Supplier;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class M2MClientFactory {
    public static final String DBAAS_AGENT_URL_PROP = "com.netcracker.cloud.dbaas.agent.url";
    public static final String MAAS_AGENT_URL_PROP = "com.netcracker.cloud.maas.agent.url";

    public static boolean isK8sM2mEnabled() {
        return Boolean.parseBoolean(System.getenv("KUBERNETES_M2M_ENABLED"));
    }

    public static OkHttpClient getM2mOkHttpClient(Supplier<String> keycloakTokenSupplier) {
        return getOkHttpClient(new M2MInterceptor(new UrlCache(), getBearerAuthHeaderSupplier(keycloakTokenSupplier), getBearerAuthHeaderSupplier(() -> KubernetesAudienceToken.getToken(AudienceName.NETCRACKER))));
    }

    public static OkHttpClient getDbaasOkHttpClient(Supplier<String> keycloakTokenSupplier) {
        return getAgentOkHttpClient(keycloakTokenSupplier, getBearerAuthHeaderSupplier(() -> KubernetesAudienceToken.getToken(AudienceName.DBAAS)), Optional.ofNullable(System.getProperty(DBAAS_AGENT_URL_PROP)).orElse("http://dbaas-agent:8080"));
    }

    public static OkHttpClient getMaasOkHttpClient(Supplier<String> keycloakTokenSupplier) {
        return getAgentOkHttpClient(keycloakTokenSupplier, getBearerAuthHeaderSupplier(() -> KubernetesAudienceToken.getToken(AudienceName.MAAS)), Optional.ofNullable(System.getProperty(MAAS_AGENT_URL_PROP)).orElse("http://maas-agent:8080"));
    }

    private static OkHttpClient getAgentOkHttpClient(Supplier<String> keycloakTokenSupplier, Supplier<String> k8sAuthHeaderSupplier, String agentUrl) {
        return getOkHttpClient(new M2MInterceptor(new UrlCache(), getBearerAuthHeaderSupplier(keycloakTokenSupplier), k8sAuthHeaderSupplier, agentUrl));
    }

    private static OkHttpClient getOkHttpClient(M2MInterceptor interceptor) {
        return new OkHttpClient.Builder()
                .addInterceptor(chain -> {
                    okhttp3.Request.Builder requestBuilder = chain.request().newBuilder();
                    RequestContextPropagation.populateResponse((key, value) -> requestBuilder.header(key, String.valueOf(value)));
                    return chain.proceed(requestBuilder.build());
                })
                .addInterceptor(interceptor)
                .build();
    }

    private static Supplier<String> getBearerAuthHeaderSupplier(Supplier<String> tokenSupplier) {
        return () -> "Bearer " + tokenSupplier.get();
    }
}

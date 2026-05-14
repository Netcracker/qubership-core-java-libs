package com.netcracker.cloud.security.core.utils.k8s;

import com.netcracker.cloud.security.core.utils.k8s.impl.M2MAuthenticator;
import com.netcracker.cloud.security.core.utils.k8s.impl.M2MHttpClient;
import com.netcracker.cloud.security.core.utils.k8s.impl.M2MInterceptor;
import com.netcracker.cloud.security.core.utils.k8s.impl.UrlCache;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import okhttp3.OkHttpClient;

import java.net.http.HttpClient;
import java.util.Optional;
import java.util.function.Supplier;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class M2MClientFactory {
    public static final String DBAAS_AGENT_URL_PROP = "com.netcracker.cloud.dbaas.agent.url";
    public static final String MAAS_AGENT_URL_PROP = "com.netcracker.cloud.maas.agent.url";

    private static final Supplier<String> k8sAuthHeaderSupplier =
            getBearerAuthHeaderSupplier(() -> KubernetesAudienceToken.getToken(AudienceName.NETCRACKER));

    public static OkHttpClient getM2mOkHttpClient(Supplier<String> keycloakTokenSupplier) {
        return getOkHttpClient(new M2MInterceptor(new UrlCache(), getBearerAuthHeaderSupplier(keycloakTokenSupplier), k8sAuthHeaderSupplier));
    }

    public static OkHttpClient getDbaasOkHttpClient(Supplier<String> keycloakTokenSupplier) {
        return getAgentOkHttpClient(keycloakTokenSupplier, Optional.ofNullable(System.getProperty(DBAAS_AGENT_URL_PROP)).orElse("http://dbaas-agent:8080"));
    }

    public static OkHttpClient getMaasOkHttpClient(Supplier<String> keycloakTokenSupplier) {
        return getAgentOkHttpClient(keycloakTokenSupplier, Optional.ofNullable(System.getProperty(MAAS_AGENT_URL_PROP)).orElse("http://maas-agent:8080"));
    }

    public static HttpClient getM2mHttpClient(Supplier<String> keycloakTokenSupplier) {
        return buildHttpClient(new M2MAuthenticator(new UrlCache(), getBearerAuthHeaderSupplier(keycloakTokenSupplier), k8sAuthHeaderSupplier));
    }

    public static HttpClient getDbaasHttpClient(Supplier<String> keycloakTokenSupplier) {
        String agentUrl = Optional.ofNullable(System.getProperty(DBAAS_AGENT_URL_PROP)).orElse("http://dbaas-agent:8080");
        return buildHttpClient(new M2MAuthenticator(new UrlCache(), getBearerAuthHeaderSupplier(keycloakTokenSupplier), k8sAuthHeaderSupplier, agentUrl));
    }

    public static HttpClient getMaasHttpClient(Supplier<String> keycloakTokenSupplier) {
        String agentUrl = Optional.ofNullable(System.getProperty(MAAS_AGENT_URL_PROP)).orElse("http://maas-agent:8080");
        return buildHttpClient(new M2MAuthenticator(new UrlCache(), getBearerAuthHeaderSupplier(keycloakTokenSupplier), k8sAuthHeaderSupplier, agentUrl));
    }

    private static OkHttpClient getAgentOkHttpClient(Supplier<String> keycloakTokenSupplier, String agentUrl) {
        return getOkHttpClient(new M2MInterceptor(new UrlCache(), getBearerAuthHeaderSupplier(keycloakTokenSupplier), k8sAuthHeaderSupplier, agentUrl));
    }

    private static OkHttpClient getOkHttpClient(M2MInterceptor interceptor) {
        return new OkHttpClient.Builder()
                .addInterceptor(interceptor)
                .build();
    }

    private static HttpClient buildHttpClient(M2MAuthenticator authenticator) {
        return new M2MHttpClient(HttpClient.newHttpClient(), authenticator);
    }

    private static Supplier<String> getBearerAuthHeaderSupplier(Supplier<String> tokenSupplier) {
        return () -> "Bearer " + tokenSupplier.get();
    }
}

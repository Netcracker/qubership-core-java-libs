package com.netcracker.cloud.dbaas.common.config;

import com.netcracker.cloud.context.propagation.core.ContextManager;
import com.netcracker.cloud.dbaas.client.DbaaSClientOkHttpImpl;
import com.netcracker.cloud.dbaas.client.DbaasClient;
import com.netcracker.cloud.framework.contexts.tenant.TenantContextObject;
import com.netcracker.cloud.security.core.utils.k8s.M2MAuthMode;
import com.netcracker.cloud.security.core.utils.tls.TlsUtils;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.Optional;

import static com.netcracker.cloud.framework.contexts.tenant.BaseTenantProvider.TENANT_CONTEXT_NAME;

@Slf4j
@ApplicationScoped
public class M2MDbaaSClient {
    private static final int MAX_RETRIES = 3;
    private static final long INITIAL_RETRY_DELAY = 500;

    private final Optional<String> apiDbaasAddress;
    private final OkHttpClient dbaasOkHttpClient;
    private final DbaasClientConfig dbaasClientConfig;

    @Inject
    public M2MDbaaSClient(@ConfigProperty(name = "api.dbaas.address") Optional<String> apiDbaasAddress,
                          @Named(DbaasClientProducer.DBAAS_HTTP_CLIENT) OkHttpClient dbaasOkHttpClient,
                          DbaasClientConfig dbaasClientConfig) {
        this.apiDbaasAddress = apiDbaasAddress;
        this.dbaasOkHttpClient = dbaasOkHttpClient;
        this.dbaasClientConfig = dbaasClientConfig;
    }

    public DbaasClient build() {
        String dbaasUrl = switch (M2MAuthMode.readFromEnv()) {
            case LEGACY -> dbaasClientConfig.dbaasAgentUrl();
            case HYBRID -> apiDbaasAddress.orElseGet(() -> {
                log.warn("DBaaS address is not available, falling back to dbaas-agent. Specify 'api.dbaas.address' property to DBaaS url");
                return dbaasClientConfig.dbaasAgentUrl();
            });
            case K8S -> apiDbaasAddress.orElseThrow(() -> new IllegalStateException(
                    "api.dbaas.address is not set: with M2M_AUTH_MODE=k8s the client sends requests directly to DBaaS, set api.dbaas.address to the DBaaS URL"));
        };

        OkHttpClient httpClient = dbaasOkHttpClient.newBuilder()
                .addInterceptor(chain -> {
                    Request original = chain.request();
                    Request.Builder requestBuilder = original.newBuilder();
                    Optional<TenantContextObject> tenantContextData = ContextManager.getSafe(TENANT_CONTEXT_NAME);
                    if (tenantContextData.isPresent() && tenantContextData.get().getTenant() != null) {
                        requestBuilder.addHeader("tenant", tenantContextData.get().getTenant());
                    }
                    return chain.proceed(requestBuilder.build());
                })
                .addInterceptor(new RetryInterceptor(MAX_RETRIES, INITIAL_RETRY_DELAY))
                .sslSocketFactory(TlsUtils.getSslContext().getSocketFactory(), TlsUtils.getTrustManager())
                .build();
        return new DbaaSClientOkHttpImpl(dbaasUrl, httpClient);
    }
}

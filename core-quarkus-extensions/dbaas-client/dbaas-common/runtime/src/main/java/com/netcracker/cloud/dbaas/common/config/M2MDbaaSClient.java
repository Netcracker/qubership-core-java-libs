package com.netcracker.cloud.dbaas.common.config;

import com.netcracker.cloud.context.propagation.core.ContextManager;
import com.netcracker.cloud.dbaas.client.DbaaSClientOkHttpImpl;
import com.netcracker.cloud.dbaas.client.DbaasClient;
import com.netcracker.cloud.framework.contexts.tenant.TenantContextObject;
import com.netcracker.cloud.quarkus.security.auth.M2MManager;
import com.netcracker.cloud.security.core.utils.k8s.M2MClientFactory;
import com.netcracker.cloud.security.core.utils.tls.TlsUtils;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;

import java.util.Optional;

import static com.netcracker.cloud.dbaas.common.config.DbaasClientConfig.DEFAULT_DBAAS_AGENT_ADDRESS;
import static com.netcracker.cloud.framework.contexts.tenant.BaseTenantProvider.TENANT_CONTEXT_NAME;

@Slf4j
@ApplicationScoped
public class M2MDbaaSClient {
    private static final int MAX_RETRIES = 3;
    private static final long INITIAL_RETRY_DELAY = 500;

    private final SecurityConfig securityConfig;
    private final DbaasClientConfig dbaasConfig;

    public M2MDbaaSClient(SecurityConfig securityConfig, DbaasClientConfig dbaasConfig) {
        this.securityConfig = securityConfig;
        this.dbaasConfig = dbaasConfig;
    }

    public DbaasClient build() {
        String dbaasAgentUrl = dbaasConfig.dbaasAgentUrl().orElse(DEFAULT_DBAAS_AGENT_ADDRESS);

        String dbaasUrl = dbaasAgentUrl;
        if(securityConfig.k8sM2mEnabled()) {
            if(dbaasConfig.dbaasUrl().isEmpty()) {
                log.warn("DBaaS address is not available, falling back to dbaas-agent. Specify 'api.dbaas.address' property to DBaaS url");
            }
            dbaasUrl = dbaasConfig.dbaasUrl().orElse(dbaasAgentUrl);
        }

        OkHttpClient httpClient = M2MClientFactory.getDbaasOkHttpClient(() -> M2MManager.getInstance().getToken().getTokenValue(), securityConfig.k8sM2mEnabled());

        httpClient = httpClient.newBuilder()
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

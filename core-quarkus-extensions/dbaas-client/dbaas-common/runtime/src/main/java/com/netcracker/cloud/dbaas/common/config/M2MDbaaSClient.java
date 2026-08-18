package com.netcracker.cloud.dbaas.common.config;

import com.netcracker.cloud.context.propagation.core.ContextManager;
import com.netcracker.cloud.dbaas.client.DbaaSClientOkHttpImpl;
import com.netcracker.cloud.dbaas.client.DbaasClient;
import com.netcracker.cloud.framework.contexts.tenant.TenantContextObject;
import com.netcracker.cloud.security.core.utils.k8s.M2MClient;
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

    @ConfigProperty(name = "api.dbaas.address")
    Optional<String> apiDbaasAddress;

    @ConfigProperty(name = "quarkus.dbaas.api.agent.url", defaultValue = DbaasClientConfig.DEFAULT_DBAAS_AGENT_ADDRESS)
    String dbaasAgentUrl;

    @Inject
    @Named(DbaasClientProducer.DBAAS_HTTP_CLIENT)
    OkHttpClient dbaasOkHttpClient;

    public DbaasClient build() {
        String dbaasUrl = dbaasAgentUrl;
        if(M2MClient.isK8sM2mEnabled()) {
            if(apiDbaasAddress.isEmpty()) {
                log.warn("DBaaS address is not available, falling back to dbaas-agent. Specify 'api.dbaas.address' property to DBaaS url");
            } else {
                dbaasUrl = apiDbaasAddress.get();
            }
        }

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

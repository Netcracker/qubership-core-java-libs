package org.qubership.cloud.dbaas.common.config;

import org.qubership.cloud.framework.contexts.tenant.TenantContextObject;
import org.qubership.cloud.context.propagation.core.ContextManager;
import org.qubership.cloud.dbaas.client.DbaaSClientOkHttpImpl;
import org.qubership.cloud.dbaas.client.DbaasClient;
import org.qubership.cloud.quarkus.security.auth.M2MManager;
import org.qubership.cloud.security.core.auth.Token;
import org.qubership.cloud.security.core.utils.tls.TlsUtils;
import jakarta.enterprise.context.ApplicationScoped;
import okhttp3.OkHttpClient;
import okhttp3.Request;

import java.util.Optional;

import static org.qubership.cloud.framework.contexts.tenant.TenantProvider.TENANT_CONTEXT_NAME;
import static org.qubership.cloud.dbaas.common.config.DbaasClientConfig.DEFAULT_DBAAS_AGENT_ADDRESS;

@ApplicationScoped
public class M2MDbaaSClient {
    private DbaasClientConfig config;
    private static final int MAX_RETRIES = 3;
    private static final long INITIAL_RETRY_DELAY = 500;

    public M2MDbaaSClient(DbaasClientConfig config) {
        this.config = config;
    }

    public DbaasClient build() {
        String url = config.dbaasAgentUrl.orElse(DEFAULT_DBAAS_AGENT_ADDRESS);
        OkHttpClient httpClient = new OkHttpClient.Builder()
                .addInterceptor(chain -> {
                    Request original = chain.request();
                    Token token = M2MManager.getInstance().getToken();
                    String credentials = token.getTokenType() + " " + token.getTokenValue();
                    Request.Builder requestBuilder = original.newBuilder()
                            .addHeader("Authorization", credentials);
                    Optional<TenantContextObject> tenantContextData = ContextManager.getSafe(TENANT_CONTEXT_NAME);
                    if (tenantContextData.isPresent() && tenantContextData.get().getTenant() != null) {
                        requestBuilder.addHeader("tenant", tenantContextData.get().getTenant());
                    }
                    return chain.proceed(requestBuilder.build());
                })
                .addInterceptor(new RetryInterceptor(MAX_RETRIES, INITIAL_RETRY_DELAY))
                .sslSocketFactory(TlsUtils.getSslContext().getSocketFactory(), TlsUtils.getTrustManager())
                .build();
        return new DbaaSClientOkHttpImpl(url, httpClient);
    }
}

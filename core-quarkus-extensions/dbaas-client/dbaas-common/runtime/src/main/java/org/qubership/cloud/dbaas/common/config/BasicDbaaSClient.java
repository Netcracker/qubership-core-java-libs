package org.qubership.cloud.dbaas.common.config;

import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.qubership.cloud.context.propagation.core.ContextManager;
import org.qubership.cloud.dbaas.client.DbaaSClientOkHttpImpl;
import org.qubership.cloud.dbaas.client.DbaasClient;
import org.qubership.cloud.framework.contexts.tenant.TenantContextObject;
import org.qubership.cloud.security.core.utils.tls.TlsUtils;

import java.util.Optional;

import static org.qubership.cloud.framework.contexts.tenant.TenantProvider.TENANT_CONTEXT_NAME;

@ApplicationScoped
@Slf4j
public class BasicDbaaSClient {
    public DbaasClientConfig config;

    public BasicDbaaSClient(DbaasClientConfig config) {
        this.config = config;
    }

    public DbaasClient build() {
        String url = config.dbaasUrl.get();
        String credentials = Credentials.basic(config.dbaasUsername.get(), config.dbaasPassword.get());
        OkHttpClient httpClient = new OkHttpClient.Builder()
                .addInterceptor(chain -> {
                    Request original = chain.request();
                    Request.Builder requestBuilder = original.newBuilder()
                            .addHeader("Authorization", credentials);
                    Optional<TenantContextObject> tenantContextData = ContextManager.getSafe(TENANT_CONTEXT_NAME);
                    if (tenantContextData.isPresent() && tenantContextData.get().getTenant() != null) {
                        requestBuilder.addHeader("tenant", tenantContextData.get().getTenant());
                    }
                    return chain.proceed(requestBuilder.build());
                })
                .sslSocketFactory(TlsUtils.getSslContext().getSocketFactory(), TlsUtils.getTrustManager())
                .build();
        return new DbaaSClientOkHttpImpl(url, httpClient);
    }
}

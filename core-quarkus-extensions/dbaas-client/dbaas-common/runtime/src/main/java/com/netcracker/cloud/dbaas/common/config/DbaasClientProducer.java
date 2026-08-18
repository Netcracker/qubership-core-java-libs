package com.netcracker.cloud.dbaas.common.config;

import com.netcracker.cloud.quarkus.security.auth.M2MManager;
import com.netcracker.cloud.security.core.utils.k8s.AudienceName;
import com.netcracker.cloud.security.core.utils.k8s.M2MClient;
import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import com.netcracker.cloud.dbaas.client.DbaasClient;

@Slf4j
@Singleton
public class DbaasClientProducer {

    public static final String DBAAS_HTTP_CLIENT = "dbaasOkHttpClient";

    @Produces
    @DefaultBean
    @Named(DBAAS_HTTP_CLIENT)
    public OkHttpClient dbaasOkHttpClient(@ConfigProperty(name = "quarkus.dbaas.api.agent.url",
            defaultValue = DbaasClientConfig.DEFAULT_DBAAS_AGENT_ADDRESS) String dbaasAgentUrl) {
        return M2MClient.builder()
                .audience(AudienceName.DBAAS)
                .agentUrl(dbaasAgentUrl)
                .keycloakTokenSupplier(() -> M2MManager.getInstance().getToken().getTokenValue())
                .build();
    }

    @Produces
    @DefaultBean
    public DbaasClient dbaaSClient(DbaasClientConfig dbaasClientConfig, M2MDbaaSClient m2mDbaaSClient) {
        if (dbaasClientConfig.dbaasUrl().isPresent() && dbaasClientConfig.dbaasUsername().isPresent() && dbaasClientConfig.dbaasPassword().isPresent()) {
            log.debug("Create dbaas client with basic auth");
            return new BasicDbaaSClient(dbaasClientConfig).build();
        }

        log.debug("Create dbaas client with m2m auth");
        return m2mDbaaSClient.build();

    }
}

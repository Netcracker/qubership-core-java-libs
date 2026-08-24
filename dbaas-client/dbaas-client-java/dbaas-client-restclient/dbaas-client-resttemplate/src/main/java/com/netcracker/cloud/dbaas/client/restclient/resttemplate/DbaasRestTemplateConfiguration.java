package com.netcracker.cloud.dbaas.client.restclient.resttemplate;

import com.netcracker.cloud.restclient.MicroserviceRestClient;
import com.netcracker.cloud.restclient.okhttp.MicroserviceOkHttpRestClient;
import com.netcracker.cloud.restlegacy.resttemplate.configuration.annotation.EnableFrameworkRestTemplate;
import com.netcracker.cloud.security.core.auth.M2MManager;
import com.netcracker.cloud.security.core.utils.k8s.AudienceName;
import com.netcracker.cloud.security.core.utils.k8s.M2MClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableFrameworkRestTemplate
@ConditionalOnProperty(value = "dbaas.restclient.resttemplate.basic-auth", havingValue = "false", matchIfMissing = true)
public class DbaasRestTemplateConfiguration {

    @Bean("dbaasRestClient")
    public MicroserviceRestClient dbaasRestClient(M2MManager m2MManager,
                                                 @Value("${dbaas.api.address:http://dbaas-agent:8080}") String dbaasAgentUrl) {
        var client = M2MClient.builder()
                .audience(AudienceName.DBAAS)
                .agentUrl(dbaasAgentUrl)
                .keycloakTokenSupplier(() -> m2MManager.getToken().getTokenValue())
                .build();
        return new MicroserviceOkHttpRestClient(client);
    }
}

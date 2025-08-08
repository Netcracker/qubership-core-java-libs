package org.qubership.cloud.maas.spring;

import org.qubership.cloud.maas.client.api.MaaSAPIClient;
import org.qubership.cloud.maas.client.impl.MaaSAPIClientImpl;
import org.qubership.cloud.security.core.auth.M2MManager;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MaaSClientConfig {

    @Bean
    @ConditionalOnMissingBean
    public MaaSAPIClient getMaaSAPIClient(M2MManager m2MManager) {
        return new MaaSAPIClientImpl(() -> m2MManager.getToken().getTokenValue());
    }

}

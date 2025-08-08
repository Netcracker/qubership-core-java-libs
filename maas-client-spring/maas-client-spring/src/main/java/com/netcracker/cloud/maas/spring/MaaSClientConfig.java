package com.netcracker.cloud.maas.spring;

import com.netcracker.cloud.maas.client.api.MaaSAPIClient;
import com.netcracker.cloud.maas.client.impl.MaaSAPIClientImpl;
import com.netcracker.cloud.security.core.auth.M2MManager;
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

package com.netcracker.cloud.dbaas.client.restclient.resttemplate;

import com.netcracker.cloud.restclient.MicroserviceRestClient;
import com.netcracker.cloud.restclient.okhttp.MicroserviceOkHttpRestClient;
import com.netcracker.cloud.restlegacy.resttemplate.configuration.annotation.EnableFrameworkRestTemplate;
import com.netcracker.cloud.security.core.auth.DummyM2MManager;
import com.netcracker.cloud.security.core.auth.M2MManager;
import com.netcracker.cloud.security.core.utils.k8s.M2MClientFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableFrameworkRestTemplate
@ConditionalOnProperty(value = "dbaas.restclient.resttemplate.basic-auth", havingValue = "false", matchIfMissing = true)
public class DbaasRestTemplateConfiguration {

    private static final Logger log = LoggerFactory.getLogger(DbaasRestTemplateConfiguration.class);

    @Bean("dbaasRestClient")
    public MicroserviceRestClient dbaasRestClient(M2MManager m2MManager) {
        var client = M2MClientFactory.getDbaasOkHttpClient(() -> m2MManager.getToken().getTokenValue());
        return new MicroserviceOkHttpRestClient(client);
    }

    @Bean
    @ConditionalOnMissingBean(M2MManager.class)
    public M2MManager dbaasRestTemplateM2MManager() {
        log.warn("Initialized dummy m2m manager for dbaas rest client. Do not use in production.");
        return new DummyM2MManager();
    }
}

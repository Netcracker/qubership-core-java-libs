package com.netcracker.cloud.dbaas.client.restclient.resttemplate;

import com.netcracker.cloud.restclient.MicroserviceRestClient;
import com.netcracker.cloud.restclient.okhttp.MicroserviceOkHttpRestClient;
import com.netcracker.cloud.restclient.resttemplate.MicroserviceRestTemplate;
import com.netcracker.cloud.restlegacy.resttemplate.configuration.annotation.EnableFrameworkRestTemplate;
import com.netcracker.cloud.security.core.auth.M2MManager;
import com.netcracker.cloud.security.core.utils.k8s.M2MClientFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
@EnableFrameworkRestTemplate
@ConditionalOnProperty(value = "dbaas.restclient.resttemplate.basic-auth", havingValue = "false", matchIfMissing = true)
public class DbaasRestTemplateConfiguration {
    @Bean("dbaasRestClient")
    public MicroserviceRestClient dbaasRestClient(M2MManager m2MManager){
        var client = M2MClientFactory.getDbaasOkHttpClient(() -> m2MManager.getToken().getTokenValue());
        return new MicroserviceOkHttpRestClient(client);
    }
}

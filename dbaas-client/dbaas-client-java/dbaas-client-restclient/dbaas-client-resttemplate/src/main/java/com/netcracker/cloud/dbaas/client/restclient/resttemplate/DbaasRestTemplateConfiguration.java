package com.netcracker.cloud.dbaas.client.restclient.resttemplate;

import com.netcracker.cloud.restclient.MicroserviceRestClient;
import com.netcracker.cloud.restclient.resttemplate.MicroserviceRestTemplate;
import com.netcracker.cloud.restlegacy.resttemplate.configuration.annotation.EnableFrameworkRestTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
@EnableFrameworkRestTemplate
@ConditionalOnProperty(value = "dbaas.restclient.resttemplate.basic-auth", havingValue = "false", matchIfMissing = true)
public class DbaasRestTemplateConfiguration {
    @Bean("dbaasRestClient")
    public MicroserviceRestClient dbaasRestClient(@Qualifier("m2mRestTemplate") RestTemplate restTemplate){
        return new MicroserviceRestTemplate(restTemplate);
    }
}

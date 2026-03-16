package com.netcracker.cloud.routeregistration.resttemplate;

import com.netcracker.cloud.restclient.MicroserviceRestClient;
import com.netcracker.cloud.restclient.resttemplate.MicroserviceRestTemplate;
import com.netcracker.cloud.restlegacy.resttemplate.configuration.annotation.EnableFrameworkRestTemplate;
import com.netcracker.cloud.routesregistration.common.config.RoutesProcessingConfiguration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.client.RestTemplate;

@Configuration
@EnableFrameworkRestTemplate
@Import(RoutesProcessingConfiguration.class)
public class RouteRegistrationRestTemplateConfiguration {

    @Bean("routeRegistrationRestClient")
    public MicroserviceRestClient configServerRestClient(@Qualifier("m2mRestTemplate") RestTemplate restTemplate){
        return new MicroserviceRestTemplate(restTemplate);
    }

}

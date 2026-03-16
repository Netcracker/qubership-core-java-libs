package com.netcracker.cloud.routeregistration.webclient;

import com.netcracker.cloud.restclient.MicroserviceRestClient;
import com.netcracker.cloud.restclient.webclient.MicroserviceWebClient;
import com.netcracker.cloud.routesregistration.common.config.RoutesProcessingConfiguration;
import com.netcracker.cloud.smartclient.config.annotation.EnableFrameworkWebClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@EnableFrameworkWebClient
@Import(RoutesProcessingConfiguration.class)
public class RouteRegistrationWebClientConfiguration {

    @Bean("routeRegistrationRestClient")
    public MicroserviceRestClient configServerRestClient(@Qualifier("m2mWebClient") WebClient webClient) {
        return new MicroserviceWebClient(webClient);
    }

}

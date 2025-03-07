package org.qubership.cloud.routeregistration.webclient;

import org.qubership.cloud.restclient.MicroserviceRestClient;
import org.qubership.cloud.restclient.webclient.MicroserviceWebClient;
import org.qubership.cloud.routesregistration.common.config.RoutesProcessingConfiguration;
import org.qubership.cloud.smartclient.config.annotation.EnableFrameworkWebClient;
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

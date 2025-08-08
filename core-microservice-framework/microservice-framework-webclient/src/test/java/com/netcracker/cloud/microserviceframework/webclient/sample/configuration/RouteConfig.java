package org.qubership.cloud.microserviceframework.webclient.sample.configuration;

import org.qubership.cloud.restclient.MicroserviceRestClient;
import org.qubership.cloud.routeregistration.webclient.EnableRouteRegistrationOnWebClient;
import org.qubership.cloud.routesregistration.common.gateway.route.ControlPlaneClient;
import org.qubership.cloud.routesregistration.common.spring.gateway.route.SpringControlPlaneClient;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
@EnableRouteRegistrationOnWebClient
public class RouteConfig {
    @Bean
    @Primary
    ControlPlaneClient controlPlaneClient(
            @Value("${apigateway.control-plane.url:http://control-plane:8080}") String controlPlaneUrl,
            @Qualifier("routeRegistrationRestClient") MicroserviceRestClient microserviceRestClient) {
        return  Mockito.spy(new SpringControlPlaneClient(controlPlaneUrl, microserviceRestClient));
    }


}

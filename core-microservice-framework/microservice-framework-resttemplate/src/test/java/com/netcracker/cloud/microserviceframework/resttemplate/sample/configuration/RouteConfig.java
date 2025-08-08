package org.qubership.cloud.microserviceframework.resttemplate.sample.configuration;

import org.qubership.cloud.restclient.MicroserviceRestClient;
import org.qubership.cloud.routeregistration.resttemplate.EnableRouteRegistrationOnRestTemplate;
import org.qubership.cloud.routesregistration.common.gateway.route.ControlPlaneClient;
import org.qubership.cloud.routesregistration.common.spring.gateway.route.SpringControlPlaneClient;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.util.StringUtils;

import java.util.Optional;

@Configuration
@EnableRouteRegistrationOnRestTemplate
public class RouteConfig {

    @Bean
    @Primary
    ControlPlaneClient controlPlaneClient(
            @Value("${apigateway.control-plane.url:http://control-plane:8080}") String controlPlaneUrl,
            @Qualifier("routeRegistrationRestClient") MicroserviceRestClient microserviceRestClient) {
        return Mockito.spy(new SpringControlPlaneClient(controlPlaneUrl, microserviceRestClient));
    }
}

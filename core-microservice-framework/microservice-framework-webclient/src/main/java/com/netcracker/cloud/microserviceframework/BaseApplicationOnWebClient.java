package com.netcracker.cloud.microserviceframework;

import com.netcracker.cloud.routeregistration.webclient.EnableRouteRegistrationOnWebClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@EnableRouteRegistrationOnWebClient
public class BaseApplicationOnWebClient extends BaseApplicationCommon {
    @Bean
    @ConditionalOnMissingBean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }
}

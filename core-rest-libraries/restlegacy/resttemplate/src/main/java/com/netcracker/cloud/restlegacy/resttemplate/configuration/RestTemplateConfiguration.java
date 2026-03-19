package com.netcracker.cloud.restlegacy.resttemplate.configuration;

import jakarta.validation.constraints.NotNull;
import com.netcracker.cloud.context.propagation.spring.resttemplate.annotation.EnableResttemplateContextProvider;
import com.netcracker.cloud.restclient.MicroserviceRestClient;
import com.netcracker.cloud.restclient.resttemplate.MicroserviceRestTemplate;
import com.netcracker.cloud.restlegacy.resttemplate.RestTemplateFactory;
import com.netcracker.cloud.security.common.restclient.OAuthRestTemplateProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * This configuration uses HttpComponentsClientHttpRequestFactory
 * as request factories for all RestTemplates provided
 * in this configuration which support ALL default HTTP methods including PATCH
 */
@Configuration
@EnableResttemplateContextProvider
@Import({ClientHttpRequestFactoryConfiguration.class, MicroserviceRestTemplateFactoryConfiguration.class})
public class RestTemplateConfiguration {
    private final List<ClientHttpRequestInterceptor> clientHttpRequestInterceptors = new ArrayList<>();
    private final ClientHttpRequestFactory clientHttpRequestFactory;

    @Autowired
    public RestTemplateConfiguration(List<ClientHttpRequestInterceptor> clientHttpRequestInterceptors,
                                     @Qualifier("clientHttpRequestFactory")
                                                        ClientHttpRequestFactory clientHttpRequestFactory) {
        if (clientHttpRequestInterceptors != null && !clientHttpRequestInterceptors.isEmpty()) {
            this.clientHttpRequestInterceptors.addAll(clientHttpRequestInterceptors);
        }
        this.clientHttpRequestFactory = clientHttpRequestFactory;
    }

    @Bean({"coreRestTemplateFactory", "restTemplateFactory"})
    public RestTemplateFactory restTemplateFactory(RestTemplateBuilder springRestTemplateBuilder, OAuthRestTemplateProvider oAuthRestTemplateProvider) {
        return new RestTemplateFactory(oAuthRestTemplateProvider,
                clientHttpRequestFactory,
                clientHttpRequestInterceptors,
                springRestTemplateBuilder
        );
    }

    @Bean
    @ConditionalOnMissingBean(OAuthRestTemplateProvider.class)
    public OAuthRestTemplateProvider oAuthRestTemplateProvider(RestTemplateBuilder restTemplateBuilder) {
        return restTemplateBuilder::build;
    }

    @Bean("restTemplate")
    public RestTemplate restTemplate(@NotNull @Qualifier("coreRestTemplateFactory") RestTemplateFactory restTemplateFactory) {
        return restTemplateFactory.getRestTemplate();
    }

    @Bean("m2mRestTemplate")
    public RestTemplate oAuthRestTemplate(@NotNull @Qualifier("coreRestTemplateFactory") RestTemplateFactory restTemplateFactory) {
        return restTemplateFactory.getM2mRestTemplate();
    }

    @ConditionalOnMissingBean(name = "m2mRestClient")
    @Bean("m2mRestClient")
    public MicroserviceRestClient m2mRestClient(@NotNull @Qualifier("coreRestTemplateFactory") RestTemplateFactory restTemplateFactory) {
        return new MicroserviceRestTemplate(restTemplateFactory.getM2mRestTemplate());
    }
}

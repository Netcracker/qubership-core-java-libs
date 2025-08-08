package com.netcracker.cloud.context.propagation.sample.requests;

import org.qubership.cloud.context.propagation.spring.resttemplate.annotation.EnableResttemplateContextProvider;
import org.qubership.cloud.context.propagation.spring.resttemplate.interceptor.SpringRestTemplateInterceptor;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

@EnableResttemplateContextProvider
public class RequestPropagationTestConfig {
    @Bean
    public RestTemplate restTemplate(SpringRestTemplateInterceptor restTemplateInterceptor) {
        return new RestTemplateBuilder()
                .additionalInterceptors(restTemplateInterceptor)
                .build();
    }
}

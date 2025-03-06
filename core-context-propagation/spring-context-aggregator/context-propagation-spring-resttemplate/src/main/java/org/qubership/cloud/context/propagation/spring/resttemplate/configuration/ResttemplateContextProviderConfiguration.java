package org.qubership.cloud.context.propagation.spring.resttemplate.configuration;

import org.qubership.cloud.context.propagation.spring.resttemplate.interceptor.SpringRestTemplateInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ResttemplateContextProviderConfiguration {
    @Bean
    public SpringRestTemplateInterceptor springRestTemplateInterceptor(){
        return new SpringRestTemplateInterceptor();
    }
}

package com.netcracker.cloud.context.propagation.spring.common.configuration;

import org.qubership.cloud.context.propagation.spring.common.filter.SpringPostAuthnContextProviderFilter;
import org.qubership.cloud.context.propagation.spring.common.filter.SpringPreAuthnContextProviderFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

@Configuration
public class SpringContextProviderConfiguration {
    @Bean
    public SpringPostAuthnContextProviderFilter springPostAuthnContextProviderFilter(){
        return new SpringPostAuthnContextProviderFilter();
    }
    @Bean
    public SpringPreAuthnContextProviderFilter springPreAuthnContextProviderFilter(){
        return new SpringPreAuthnContextProviderFilter();
    }

    @Value("${headers.allowed:}")
    private String allowedHeaders;


    @PostConstruct
    public void init() {
        System.setProperty("headers.allowed", allowedHeaders);
    }
}

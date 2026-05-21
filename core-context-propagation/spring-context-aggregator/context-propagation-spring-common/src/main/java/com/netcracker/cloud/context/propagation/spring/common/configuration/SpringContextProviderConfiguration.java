package com.netcracker.cloud.context.propagation.spring.common.configuration;

import com.netcracker.cloud.context.propagation.spring.common.filter.SpringPostAuthnContextProviderFilter;
import com.netcracker.cloud.context.propagation.spring.common.filter.SpringPreAuthnContextProviderFilter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import jakarta.annotation.PostConstruct;

@Configuration
public class SpringContextProviderConfiguration {

    @Bean
    public SpringPostAuthnContextProviderFilter springPostAuthnContextProviderFilter() {
        return new SpringPostAuthnContextProviderFilter();
    }

    @Bean
    public SpringPreAuthnContextProviderFilter springPreAuthnContextProviderFilter() {
        return new SpringPreAuthnContextProviderFilter();
    }

    @SuppressWarnings("java:S3305")
    @Autowired
    private Environment environment;

    private static final String ENABLE_OPTIONAL_PROPERTY = "context.propagation.headers.enable.optional";
 
    @Value("${headers.allowed:}")
    private String allowedHeaders;

    @Value("${" + ENABLE_OPTIONAL_PROPERTY + ":}")
    private String enableOptional;

    @PostConstruct
    public void init() {
        System.setProperty("headers.allowed", allowedHeaders);
        if (environment.containsProperty(ENABLE_OPTIONAL_PROPERTY)) {
            System.setProperty(ENABLE_OPTIONAL_PROPERTY, enableOptional);
        }
    }
}

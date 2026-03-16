package com.netcracker.cloud.restlegacy.resttemplate.app;

import com.netcracker.cloud.restlegacy.resttemplate.configuration.RestTemplateConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.boot.security.autoconfigure.SecurityProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

import java.util.Properties;

@Configuration
@Import(RestTemplateConfiguration.class)
public class TestConfig {

    @Bean
    @ConditionalOnMissingBean
    public RestTemplateBuilder restTemplateBuilder() {
        return new RestTemplateBuilder();
    }

    @ConditionalOnMissingBean
    @Bean
    public SecurityProperties securityProperties() {
        return new SecurityProperties();
    }

    @Bean(name = "microserviceCoreConfigServerProperties")
    public PropertySourcesPlaceholderConfigurer microserviceCoreConfigServerProperties() {
        final PropertySourcesPlaceholderConfigurer configurer = new PropertySourcesPlaceholderConfigurer();
        Properties properties = new Properties();
        properties.setProperty("cloud.microservice.name", "test-app");
        properties.setProperty("policy.update.enabled", "false");
        configurer.setProperties(properties);
        configurer.setIgnoreUnresolvablePlaceholders(true);
        return configurer;
    }
}

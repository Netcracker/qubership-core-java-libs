package org.qubership.cloud.smartclient.rest.webclient.sample;

import org.qubership.cloud.smartclient.config.annotation.EnableFrameworkWebClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

import java.util.Properties;

@Configuration
@EnableFrameworkWebClient
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
    static public PropertySourcesPlaceholderConfigurer microserviceCoreConfigServerProperties() {
        final PropertySourcesPlaceholderConfigurer configurer = new PropertySourcesPlaceholderConfigurer();
        Properties properties = new Properties();
        properties.setProperty("cloud.microservice.name", "test-app");
        properties.setProperty("policy.update.enabled", "false");
        configurer.setProperties(properties);
        configurer.setIgnoreUnresolvablePlaceholders(true);
        return configurer;
    }

}

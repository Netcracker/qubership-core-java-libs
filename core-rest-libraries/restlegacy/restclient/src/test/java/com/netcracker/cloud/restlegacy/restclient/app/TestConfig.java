package com.netcracker.cloud.restlegacy.restclient.app;

import com.netcracker.cloud.restlegacy.restclient.ApiGatewayClient;
import com.netcracker.cloud.restlegacy.restclient.retry.RetryTemplateBuilder;
import com.netcracker.cloud.restlegacy.resttemplate.configuration.RestTemplateConfiguration;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.boot.security.autoconfigure.SecurityProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

import java.util.Properties;

@Configuration
@Import({RestTemplateConfiguration.class})
public class TestConfig {

    @Bean
    public RestTemplateBuilder restTemplateBuilder() {
        return new RestTemplateBuilder();
    }

    @Bean
    public SecurityProperties securityProperties() {
        return new SecurityProperties();
    }

    @Bean
    public ApiGatewayClient testApiGatewayClient() {
        return new ApiGatewayClient(1, "test-app", new RetryTemplateBuilder().withNeverRetryPolicy().build());
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

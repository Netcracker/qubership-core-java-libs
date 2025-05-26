package org.qubership.cloud.restlegacy.restclient.app;

import org.qubership.cloud.restlegacy.restclient.ApiGatewayClient;
import org.qubership.cloud.restlegacy.restclient.retry.RetryTemplateBuilder;
import org.qubership.cloud.restlegacy.resttemplate.configuration.RestTemplateConfiguration;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
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

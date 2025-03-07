package org.qubership.cloud.restlegacy.restclient.configuration;

import org.qubership.cloud.restlegacy.restclient.ApiGatewayClient;
import org.qubership.cloud.restlegacy.restclient.RestClient;
import org.qubership.cloud.restlegacy.restclient.retry.RetryTemplateBuilder;
import org.qubership.cloud.restlegacy.resttemplate.RestTemplateFactory;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.RestTemplate;

import java.util.Properties;

@Configuration
public class ClientsTestConfiguration {

    @Bean("restTemplate")
    public static RestTemplate restTemplate() {
        return Mockito.mock(RestTemplate.class);
    }

    @Bean
    public static RestTemplateFactory restTemplateFactory(@Qualifier("restTemplate") RestTemplate restTemplate) {
        RestTemplateFactory mock = Mockito.mock(RestTemplateFactory.class);
        Mockito.when(mock.getRestTemplate()).thenReturn(restTemplate);
        return mock;
    }

    @Bean
    public static RetryTemplate retryTemplate() {
        return Mockito.spy(new RetryTemplateBuilder()
                .withNeverRetryPolicy()
                .build());
    }

    @Bean
    public static PropertySourcesPlaceholderConfigurer properties() {
        final PropertySourcesPlaceholderConfigurer pspc = new PropertySourcesPlaceholderConfigurer();
        Properties properties = new Properties();

        properties.setProperty("apigateway.url", "http://api-gateway");

        pspc.setProperties(properties);
        return pspc;
    }

    @Bean
    public static ApiGatewayClient apiGatewayClient(RetryTemplate retryTemplate) {
        Integer apiVersion = 1;
        String appName = "some-app";
        return new ApiGatewayClient(apiVersion, appName, retryTemplate) {
        };
    }

    @Bean
    public static RestClient restClient(RetryTemplate retryTemplate) {
        return new RestClient(retryTemplate) {
        };
    }
}
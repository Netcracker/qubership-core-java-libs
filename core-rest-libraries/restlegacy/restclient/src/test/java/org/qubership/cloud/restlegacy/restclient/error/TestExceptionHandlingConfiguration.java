package org.qubership.cloud.restlegacy.restclient.error;

import org.qubership.cloud.restlegacy.restclient.RestClient;
import org.qubership.cloud.restlegacy.restclient.app.TestConfig;
import org.qubership.cloud.restlegacy.restclient.configuration.annotation.EnableControllersAdvice;
import org.qubership.cloud.restlegacy.restclient.configuration.annotation.EnableDefaultRetryTemplate;
import org.qubership.cloud.restlegacy.restclient.configuration.annotation.EnableMessagesResolving;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.embedded.undertow.UndertowServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.retry.stats.DefaultStatisticsRepository;
import org.springframework.retry.stats.StatisticsListener;
import org.springframework.retry.stats.StatisticsRepository;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriTemplateHandler;

import jakarta.annotation.PostConstruct;
import java.net.URI;
import java.util.Collections;
import java.util.Properties;

@ComponentScan(basePackageClasses = TestExceptionHandlingRestController.class)
@Configuration
@EnableMessagesResolving
@EnableControllersAdvice
@EnableDefaultRetryTemplate
@SuppressWarnings("unused")
@Import({TestConfig.class, WebMvcAutoConfiguration.class, HttpMessageConvertersAutoConfiguration.class, DispatcherServletAutoConfiguration.class})

public class TestExceptionHandlingConfiguration {
    @ConditionalOnMissingBean
    @Bean
    public static PropertySourcesPlaceholderConfigurer properties() {
        final PropertySourcesPlaceholderConfigurer pspc = new PropertySourcesPlaceholderConfigurer();
        Properties properties = new Properties();

        properties.setProperty("cloud.microservice.name", "test-app");
        properties.setProperty("apigateway.url", "http://api-gateway");

        pspc.setProperties(properties);
        return pspc;
    }

    @Bean
    public ServletWebServerFactory servletContainerFactory() {
        UndertowServletWebServerFactory factory = new UndertowServletWebServerFactory();
        factory.setPort(0);
        return factory;
    }

    @Bean
    public StatisticsRepository retryStatistic() {
        return new DefaultStatisticsRepository();
    }

    @Component
    public static class TestRestClient extends RestClient {
        private final UriTemplateHandler uriTemplateHandler;

        @Autowired
        public TestRestClient(RetryTemplate retryTemplate, TestRestTemplate template, StatisticsRepository statisticsRepository) {
            super(retryTemplate);
            retryTemplate.registerListener(new StatisticsListener(statisticsRepository));
            this.uriTemplateHandler = template.getRestTemplate().getUriTemplateHandler();
        }

        @PostConstruct
        private void fixToClearClientInterceptors() {
            getRestTemplate().setInterceptors(Collections.emptyList());
        }

        public String sendRequest(String relatedUrl) {
            final URI requestUrl = uriTemplateHandler.expand(relatedUrl);
            return get(requestUrl.toString(), String.class).getBody();
        }

        public String safelySendRequest(String relatedUrl) {
            try {
                return sendRequest(relatedUrl);
            } catch (Exception ex) {
            }
            return null;
        }
    }
}

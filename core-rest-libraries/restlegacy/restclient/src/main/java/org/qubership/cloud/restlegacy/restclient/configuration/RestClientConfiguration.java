package org.qubership.cloud.restlegacy.restclient.configuration;

import org.qubership.cloud.restlegacy.restclient.RestClient;
import org.qubership.cloud.restlegacy.resttemplate.configuration.annotation.EnableFrameworkRestTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.retry.support.RetryTemplate;

@Configuration
@EnableFrameworkRestTemplate
@Import(DefaultRetryTemplateConfiguration.class)
public class RestClientConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public RestClient restClient(RetryTemplate retryTemplate) {
        return new RestClient(retryTemplate);
    }

}

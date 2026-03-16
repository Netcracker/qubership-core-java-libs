package com.netcracker.cloud.restlegacy.restclient.configuration;

import com.netcracker.cloud.restlegacy.restclient.retry.RetryTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.retry.support.RetryTemplate;

@Configuration
public class DefaultRetryTemplateConfiguration {

    @Bean
    @Primary
    public static RetryTemplate defaultRetryTemplate() {
        return new RetryTemplateBuilder()
                .withSimpleRetryPolicy()
                .withTimeoutRetryPolicy()
                .build();
    }
}

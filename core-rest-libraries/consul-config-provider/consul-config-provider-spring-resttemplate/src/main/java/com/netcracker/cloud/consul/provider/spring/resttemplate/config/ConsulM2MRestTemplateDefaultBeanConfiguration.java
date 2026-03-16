package com.netcracker.cloud.consul.provider.spring.resttemplate.config;

import com.netcracker.cloud.consul.provider.common.TokenStorage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class ConsulM2MRestTemplateDefaultBeanConfiguration {

    @Bean
    @ConditionalOnProperty(value = "spring.cloud.consul.config.m2m.enabled", havingValue = "false")
    public TokenStorage defaultbeanConsulTokenStorageViaM2MRestTemplate() {
        return new TokenStorage() {
            @Override
            public String get() {
                return "";
            }

            @Override
            public void update(String s) {
                // nothing
            }
        };
    }
}

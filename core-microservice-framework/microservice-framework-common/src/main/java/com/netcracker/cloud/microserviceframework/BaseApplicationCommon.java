package com.netcracker.cloud.microserviceframework;

import com.netcracker.cloud.frameworkextensions.metrics.config.annotation.EnableCustomMetrics;
import com.netcracker.cloud.microserviceframework.config.MicroserviceFrameworkConfiguration;
import com.netcracker.cloud.microserviceframework.config.annotation.EnableMSInfoProvider;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.config.client.ConfigClientAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

import static org.springframework.context.annotation.FilterType.REGEX;

@Configuration
@EnableAutoConfiguration(exclude = ConfigClientAutoConfiguration.class)
@ComponentScan(value = "com.netcracker.cloud",
        excludeFilters = {
                @ComponentScan.Filter(type = REGEX, pattern = "com.netcracker.cloud.microserviceframework.*"),
                @ComponentScan.Filter(type = REGEX, pattern = "com.netcracker.cloud.securitycore.*"),
                @ComponentScan.Filter(type = REGEX, pattern = "com.netcracker.cloud.dbaas.client.*")})
@EnableCustomMetrics
@EnableMSInfoProvider
@SpringBootConfiguration(proxyBeanMethods = false)
@Import({MicroserviceFrameworkConfiguration.class})
abstract class BaseApplicationCommon {
    @Bean
    @ConditionalOnMissingBean
    SecurityFilterChain securityFilterChain(HttpSecurity http) {
        return http
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .csrf(AbstractHttpConfigurer::disable)
                .build();
    }
}

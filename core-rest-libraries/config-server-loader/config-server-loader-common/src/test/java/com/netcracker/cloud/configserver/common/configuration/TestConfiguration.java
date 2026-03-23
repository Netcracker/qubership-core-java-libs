package com.netcracker.cloud.configserver.common.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class TestConfiguration {

    public static final String CONTEXT_REFRESH_AMOUNT_PROP_KEY = "test.context.refresh.max";

    public static final String REFRESH_EVENT_AMOUNT_PROP_KEY = "test.refresh.max";

    @Bean
    public BootstrapStateEventListeners.ContextRefreshedEventListener contextRefreshedEventListenerAsserter(
            @Value("${" + CONTEXT_REFRESH_AMOUNT_PROP_KEY + "}") String maxAmountOfRefreshes) {
        return new BootstrapStateEventListeners.ContextRefreshedEventListener(Integer.parseInt(maxAmountOfRefreshes));
    }

    @Bean
    public BootstrapStateEventListeners.EnvironmentChangedAfterRefreshEventListener refreshEventListenerAsserter(
            @Value("${" + REFRESH_EVENT_AMOUNT_PROP_KEY + "}") String maxAmountOfRefreshes) {
        return new BootstrapStateEventListeners.EnvironmentChangedAfterRefreshEventListener(Integer.parseInt(maxAmountOfRefreshes));
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .csrf(AbstractHttpConfigurer::disable)
                .build();
    }
}

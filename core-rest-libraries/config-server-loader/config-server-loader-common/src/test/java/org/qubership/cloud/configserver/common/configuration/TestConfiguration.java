package org.qubership.cloud.configserver.common.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
}

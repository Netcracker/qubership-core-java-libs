package com.netcracker.cloud.consul.provider.spring.webclient.config;

import com.netcracker.cloud.consul.provider.common.ConsulLoginMode;
import com.netcracker.cloud.consul.provider.common.TokenStorageFactory;
import com.netcracker.cloud.consul.provider.spring.common.config.ConsulLoginProperties;
import com.netcracker.cloud.security.common.reactive.M2MManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.cloud.consul.ConsulProperties;

/**
 * Covers the wiring this module owns. How the login properties themselves turn into options is checked once, in
 * {@code ConsulLoginPropertiesTest}.
 */
class ConsulM2MWebClientAutoConfigurationTest {

    private final ConsulLoginProperties loginProperties = new ConsulLoginProperties();
    private ConsulProperties consulProperties;
    private M2MManager m2MManager;

    @BeforeEach
    void init() {
        consulProperties = new ConsulProperties();
        consulProperties.setHost("consul");
        consulProperties.setPort(8500);

        m2MManager = Mockito.mock(M2MManager.class);
    }

    private TokenStorageFactory.CreateOptions options() {
        return ConsulM2MWebClientAutoConfiguration.createOptions(loginProperties, consulProperties, m2MManager, "ns");
    }

    @Test
    void loginPropertiesReachTheOptions() {
        loginProperties.setMode(ConsulLoginMode.KUBERNETES);
        loginProperties.setAuthMethod("core-k8s");

        TokenStorageFactory.CreateOptions opts = options();

        Assertions.assertEquals(ConsulLoginMode.KUBERNETES, opts.getMode());
        Assertions.assertEquals("core-k8s", opts.getAuthMethod());
    }

    @Test
    void buildingTheOptionsAsksNoM2MTokenInAnyMode() {
        for (ConsulLoginMode mode : ConsulLoginMode.values()) {
            loginProperties.setMode(mode);

            Assertions.assertEquals(mode, options().getMode());
        }

        Mockito.verifyNoInteractions(m2MManager);
    }
}

package com.netcracker.cloud.consul.provider.spring.webclient.config;

import com.netcracker.cloud.consul.provider.common.ConsulLoginMode;
import com.netcracker.cloud.consul.provider.common.TokenStorageFactory;
import com.netcracker.cloud.consul.provider.spring.common.config.ConsulLoginProperties;
import com.netcracker.cloud.security.common.reactive.M2MManager;
import com.netcracker.cloud.security.core.utils.k8s.AudienceName;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.cloud.consul.ConsulProperties;

import java.time.Duration;

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
    void modeIsKubernetesWithM2MFallbackWhenNothingIsConfigured() {
        Assertions.assertEquals(ConsulLoginMode.KUBERNETES_WITH_M2M_FALLBACK, options().getMode());
    }

    @Test
    void kubernetesModeReachesCreateOptionsAndNeedsNoM2MManager() {
        loginProperties.setMode(ConsulLoginMode.KUBERNETES);

        TokenStorageFactory.CreateOptions opts = options();

        Assertions.assertEquals(ConsulLoginMode.KUBERNETES, opts.getMode());
        Mockito.verifyNoInteractions(m2MManager);
    }

    @Test
    void m2mModeReachesCreateOptions() {
        loginProperties.setMode(ConsulLoginMode.M2M);

        Assertions.assertEquals(ConsulLoginMode.M2M, options().getMode());
    }

    @Test
    void authMethodAndAudienceDefaultsComeFromTheBuilder() {
        loginProperties.setMode(ConsulLoginMode.KUBERNETES);

        TokenStorageFactory.CreateOptions opts = options();

        Assertions.assertEquals(TokenStorageFactory.CreateOptions.DEFAULT_AUTH_METHOD, opts.getAuthMethod());
        Assertions.assertEquals(AudienceName.NETCRACKER, opts.getAudience());
    }

    @Test
    void authMethodAndAudienceAreReadFromTheConfiguration() {
        loginProperties.setMode(ConsulLoginMode.KUBERNETES);
        loginProperties.setAuthMethod("core-k8s");
        loginProperties.setAudience(AudienceName.DBAAS);

        TokenStorageFactory.CreateOptions opts = options();

        Assertions.assertEquals("core-k8s", opts.getAuthMethod());
        Assertions.assertEquals(AudienceName.DBAAS, opts.getAudience());
    }

    @Test
    void fallbackRecheckIntervalDefaultComesFromTheBuilder() {
        Assertions.assertEquals(TokenStorageFactory.CreateOptions.DEFAULT_FALLBACK_RECHECK_INTERVAL,
                options().getFallbackRecheckInterval());
    }

    @Test
    void fallbackRecheckIntervalIsReadFromTheConfiguration() {
        loginProperties.setFallbackRecheckInterval(Duration.ofMinutes(30));

        Assertions.assertEquals(Duration.ofMinutes(30), options().getFallbackRecheckInterval());
    }
}

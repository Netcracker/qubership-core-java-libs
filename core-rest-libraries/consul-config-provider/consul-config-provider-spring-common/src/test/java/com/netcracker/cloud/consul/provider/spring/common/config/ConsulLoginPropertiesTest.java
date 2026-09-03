package com.netcracker.cloud.consul.provider.spring.common.config;

import com.netcracker.cloud.consul.provider.common.ConsulLoginMode;
import com.netcracker.cloud.consul.provider.common.TokenStorageFactory;
import com.netcracker.cloud.security.core.utils.k8s.AudienceName;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Duration;

class ConsulLoginPropertiesTest {

    private static final String CONSUL_URL = "http://consul:8500";

    private final ConsulLoginProperties loginProperties = new ConsulLoginProperties();

    private TokenStorageFactory.CreateOptions options() {
        return loginProperties.toOptionsBuilder()
                .consulUrl(CONSUL_URL)
                .namespace("ns")
                .m2mSupplier(() -> "m2m-token")
                .build();
    }

    @Test
    void modeIsKubernetesWithM2MFallbackWhenNothingIsConfigured() {
        Assertions.assertEquals(ConsulLoginMode.KUBERNETES_WITH_M2M_FALLBACK, options().getMode());
    }

    @Test
    void everyModeReachesTheOptions() {
        for (ConsulLoginMode mode : ConsulLoginMode.values()) {
            loginProperties.setMode(mode);

            Assertions.assertEquals(mode, options().getMode());
        }
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

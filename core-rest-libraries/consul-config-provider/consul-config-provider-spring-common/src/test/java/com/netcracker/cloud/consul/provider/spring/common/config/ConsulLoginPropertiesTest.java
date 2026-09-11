package com.netcracker.cloud.consul.provider.spring.common.config;

import com.netcracker.cloud.consul.provider.common.ConsulLoginMode;
import com.netcracker.cloud.consul.provider.common.TokenStorageFactory;
import com.netcracker.cloud.security.core.utils.k8s.AudienceName;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;

import java.time.Duration;
import java.util.Map;

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
        loginProperties.setMethod("core-k8s");
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

    private static void assertBoundValues(ConsulLoginProperties bound) {
        TokenStorageFactory.CreateOptions opts = bound.toOptionsBuilder()
                .consulUrl(CONSUL_URL)
                .namespace("ns")
                .m2mSupplier(() -> "m2m-token")
                .build();

        Assertions.assertEquals(ConsulLoginMode.M2M, opts.getMode(), "mode");
        Assertions.assertEquals("core-k8s", opts.getAuthMethod(), "auth method");
        Assertions.assertEquals(AudienceName.DBAAS, opts.getAudience(), "audience");
        Assertions.assertEquals(Duration.ofMinutes(30), opts.getFallbackRecheckInterval(), "recheck interval");
    }

    @Test
    void everyPropertyNameTheDocumentationGivesBinds() {
        MapConfigurationPropertySource source = new MapConfigurationPropertySource(Map.of(
                "consul.auth.mode", "m2m",
                "consul.auth.method", "core-k8s",
                "consul.auth.audience", AudienceName.DBAAS,
                "consul.auth.fallback-recheck-interval", "30m"));

        assertBoundValues(new Binder(source).bind(ConsulLoginProperties.PREFIX, ConsulLoginProperties.class)
                .get());
    }

    /**
     * The environment variable names are the ones the go and the Quarkus stacks read as well, so a deployment sets
     * the same four whatever the stack. The interval carries the hyphenated property name here and the dotted one in
     * go, and this pins that both are reachable from one variable.
     */
    @Test
    void everyEnvironmentVariableNameTheDocumentationGivesBinds() {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new SystemEnvironmentPropertySource("systemEnvironment", Map.<String, Object>of(
                "CONSUL_AUTH_MODE", "m2m",
                "CONSUL_AUTH_METHOD", "core-k8s",
                "CONSUL_AUTH_AUDIENCE", AudienceName.DBAAS,
                "CONSUL_AUTH_FALLBACK_RECHECK_INTERVAL", "30m")));

        assertBoundValues(Binder.get(environment).bind(ConsulLoginProperties.PREFIX, ConsulLoginProperties.class)
                .get());
    }
}

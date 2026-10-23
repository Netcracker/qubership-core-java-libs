package com.netcracker.cloud.consul.provider.spring.common.config;

import com.netcracker.cloud.consul.provider.common.ConsulLoginMode;
import com.netcracker.cloud.consul.provider.common.TokenStorageFactory;
import com.netcracker.cloud.security.core.utils.k8s.AudienceName;
import com.netcracker.cloud.security.core.utils.k8s.M2MAuthMode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

import java.time.Duration;
import java.util.Map;

@ExtendWith(SystemStubsExtension.class)
class ConsulLoginPropertiesTest {

    private static final String CONSUL_URL = "http://consul:8500";

    private final ConsulLoginProperties loginProperties = new ConsulLoginProperties();

    @SystemStub
    private EnvironmentVariables environmentVariables;

    /**
     * The builder takes an absent mode from {@code M2M_AUTH_MODE}, so the machine running the tests must not decide it.
     */
    @BeforeEach
    void removeM2MAuthMode() {
        environmentVariables.remove(M2MAuthMode.M2M_AUTH_MODE_ENV);
    }

    private TokenStorageFactory.CreateOptions options() {
        return loginProperties.toOptionsBuilder()
                .consulUrl(CONSUL_URL)
                .namespace("ns")
                .m2mSupplier(() -> "m2m-token")
                .build();
    }

    @Test
    void modeIsLegacyWhenNothingIsConfigured() {
        Assertions.assertEquals(ConsulLoginMode.LEGACY, options().getMode());
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
        loginProperties.setMode(ConsulLoginMode.K8S);

        TokenStorageFactory.CreateOptions opts = options();

        Assertions.assertEquals(TokenStorageFactory.CreateOptions.DEFAULT_AUTH_METHOD, opts.getAuthMethod());
        Assertions.assertEquals(AudienceName.NETCRACKER, opts.getAudience());
    }

    @Test
    void authMethodAndAudienceAreReadFromTheConfiguration() {
        loginProperties.setMode(ConsulLoginMode.K8S);
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

        Assertions.assertEquals(ConsulLoginMode.K8S, opts.getMode(), "mode");
        Assertions.assertEquals("core-k8s", opts.getAuthMethod(), "auth method");
        Assertions.assertEquals(AudienceName.DBAAS, opts.getAudience(), "audience");
        Assertions.assertEquals(Duration.ofMinutes(30), opts.getFallbackRecheckInterval(), "recheck interval");
    }

    @Test
    void everyPropertyNameTheDocumentationGivesBinds() {
        MapConfigurationPropertySource source = new MapConfigurationPropertySource(Map.of(
                "consul.auth.mode", "k8s",
                "consul.auth.method", "core-k8s",
                "consul.auth.audience", AudienceName.DBAAS,
                "consul.auth.fallback-recheck-interval", "30m"));

        assertBoundValues(new Binder(source).bind(ConsulLoginProperties.PREFIX, ConsulLoginProperties.class)
                .get());
    }

    private static ConsulLoginProperties bindWithEmptyModeVariable() {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new SystemEnvironmentPropertySource("systemEnvironment", Map.<String, Object>of(
                "CONSUL_AUTH_MODE", "",
                "CONSUL_AUTH_METHOD", "core-k8s")));

        return Binder.get(environment)
                .bind(ConsulLoginProperties.PREFIX, ConsulLoginProperties.class)
                .get();
    }

    @Test
    void anEmptyModeVariableIsBoundAsAbsent() {
        Assertions.assertNull(bindWithEmptyModeVariable().getMode());
    }

    @Test
    void anEmptyModeVariableTakesTheModeOfM2MAuthMode() {
        environmentVariables.set(M2MAuthMode.M2M_AUTH_MODE_ENV, "k8s");

        Assertions.assertEquals(ConsulLoginMode.K8S, bindWithEmptyModeVariable().toOptionsBuilder()
                .consulUrl(CONSUL_URL)
                .namespace("ns")
                .m2mSupplier(() -> "m2m-token")
                .build()
                .getMode());
    }

    @Test
    void anEmptyModeVariableIsLegacyWhenM2MAuthModeIsUnsetToo() {
        Assertions.assertEquals(ConsulLoginMode.LEGACY, bindWithEmptyModeVariable().toOptionsBuilder()
                .consulUrl(CONSUL_URL)
                .namespace("ns")
                .m2mSupplier(() -> "m2m-token")
                .build()
                .getMode());
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
                "CONSUL_AUTH_MODE", "k8s",
                "CONSUL_AUTH_METHOD", "core-k8s",
                "CONSUL_AUTH_AUDIENCE", AudienceName.DBAAS,
                "CONSUL_AUTH_FALLBACK_RECHECK_INTERVAL", "30m")));

        assertBoundValues(Binder.get(environment).bind(ConsulLoginProperties.PREFIX, ConsulLoginProperties.class)
                .get());
    }
}

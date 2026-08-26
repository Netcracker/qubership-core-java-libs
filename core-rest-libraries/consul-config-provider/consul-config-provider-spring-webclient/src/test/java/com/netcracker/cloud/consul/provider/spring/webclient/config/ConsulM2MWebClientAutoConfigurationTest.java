package com.netcracker.cloud.consul.provider.spring.webclient.config;

import com.netcracker.cloud.consul.provider.common.ConsulLoginMode;
import com.netcracker.cloud.consul.provider.common.TokenStorageFactory;
import com.netcracker.cloud.security.common.reactive.M2MManager;
import com.netcracker.cloud.security.core.utils.k8s.AudienceName;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.context.properties.bind.BindException;
import org.springframework.cloud.consul.ConsulProperties;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

import java.util.HashMap;
import java.util.Map;

import static com.netcracker.cloud.consul.provider.spring.webclient.config.ConsulM2MWebClientAutoConfiguration.PROP_LOGIN_AUDIENCE;
import static com.netcracker.cloud.consul.provider.spring.webclient.config.ConsulM2MWebClientAutoConfiguration.PROP_LOGIN_AUTH_METHOD;
import static com.netcracker.cloud.consul.provider.spring.webclient.config.ConsulM2MWebClientAutoConfiguration.PROP_LOGIN_MODE;

class ConsulM2MWebClientAutoConfigurationTest {

    private final Map<String, Object> properties = new HashMap<>();
    private StandardEnvironment environment;
    private ConsulProperties consulProperties;
    private M2MManager m2MManager;

    @BeforeEach
    void init() {
        environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("test", properties));

        consulProperties = new ConsulProperties();
        consulProperties.setHost("consul");
        consulProperties.setPort(8500);

        m2MManager = Mockito.mock(M2MManager.class);
    }

    private TokenStorageFactory.CreateOptions options() {
        return ConsulM2MWebClientAutoConfiguration.createOptions(environment, consulProperties, m2MManager, "ns");
    }

    @Test
    void modeIsAutoWhenNothingIsConfigured() {
        Assertions.assertEquals(ConsulLoginMode.AUTO, options().getMode());
    }

    @Test
    void kubernetesModeReachesCreateOptionsAndNeedsNoM2MManager() {
        properties.put(PROP_LOGIN_MODE, "kubernetes");

        TokenStorageFactory.CreateOptions opts = options();

        Assertions.assertEquals(ConsulLoginMode.KUBERNETES, opts.getMode());
        Mockito.verifyNoInteractions(m2MManager);
    }

    @Test
    void unknownModeBreaksTheBean() {
        properties.put(PROP_LOGIN_MODE, "cloud-foundry");

        Assertions.assertThrows(BindException.class, this::options);
    }

    @Test
    void authMethodAndAudienceDefaultsComeFromTheBuilder() {
        properties.put(PROP_LOGIN_MODE, "kubernetes");

        TokenStorageFactory.CreateOptions opts = options();

        Assertions.assertEquals(TokenStorageFactory.CreateOptions.DEFAULT_AUTH_METHOD, opts.getAuthMethod());
        Assertions.assertEquals(AudienceName.NETCRACKER, opts.getAudience());
    }

    @Test
    void authMethodAndAudienceAreReadFromTheConfiguration() {
        properties.put(PROP_LOGIN_MODE, "kubernetes");
        properties.put(PROP_LOGIN_AUTH_METHOD, "core-k8s");
        properties.put(PROP_LOGIN_AUDIENCE, AudienceName.DBAAS);

        TokenStorageFactory.CreateOptions opts = options();

        Assertions.assertEquals("core-k8s", opts.getAuthMethod());
        Assertions.assertEquals(AudienceName.DBAAS, opts.getAudience());
    }
}

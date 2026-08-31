package com.netcracker.cloud.consul.provider.spring.common.config;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.util.Map;
import com.netcracker.cloud.consul.provider.common.ConsulLoginCredentials;
import com.netcracker.cloud.consul.provider.common.client.ConsulClientResponse;
import com.netcracker.cloud.consul.provider.common.client.ConsulRestClient;
import com.netcracker.cloud.restclient.MicroserviceRestClient;
import com.netcracker.cloud.security.core.auth.M2MManager;
import com.netcracker.cloud.security.core.auth.Token;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;
import org.springframework.boot.bootstrap.DefaultBootstrapContext;
import org.springframework.boot.bootstrap.BootstrapRegistry;
import org.springframework.boot.context.config.ConfigDataLocationResolverContext;
import org.springframework.boot.context.properties.bind.BindException;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.boot.logging.DeferredLogFactory;
import org.springframework.cloud.consul.ConsulProperties;
import org.springframework.cloud.consul.config.ConsulConfigProperties;
import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import static com.netcracker.cloud.consul.provider.spring.common.config.ConsulM2MConfigDataLocationResolver.PROP_CLOUD_NAMESPACE;
import static com.netcracker.cloud.consul.provider.spring.common.config.ConsulM2MConfigDataLocationResolver.PROP_CONSUL_M2M_ENABLED;

import static com.netcracker.cloud.consul.provider.spring.common.config.ConsulM2MConfigDataLocationResolver.args;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConsulM2MConfigDataLocationResolverTest {

    @Test
    void getPropsOrEnvsMust() {
        assertThrows(IllegalArgumentException.class, () -> ConsulM2MConfigDataLocationResolver.getPropsOrEnvsMust(args(""), args("")));
        assertThrows(IllegalArgumentException.class, () -> ConsulM2MConfigDataLocationResolver.getPropsOrEnvsMust(args("not.exists"), args("")));
        assertThrows(IllegalArgumentException.class, () -> ConsulM2MConfigDataLocationResolver.getPropsOrEnvsMust(args(""), args("NOT_EXISTS")));

        System.setProperty("my.property", "property-value");
        String val = ConsulM2MConfigDataLocationResolver.getPropsOrEnvsMust(args("my.property"), args(""));
        assertEquals("property-value", val);
        System.clearProperty("my.property");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> ConsulM2MConfigDataLocationResolver.getPropsOrEnvsMust(args("first.property", "second.property"), args("ENV_FIRST_PROPERTY", "ENV_SECOND_PROPERTY")));
        assertEquals("Missing required prop(s): [first.property, second.property] or env(s): [ENV_FIRST_PROPERTY, ENV_SECOND_PROPERTY]", ex.getMessage());
    }

    @Test
    void getPropsOrEnvsMust_envs() {
        Map.Entry<String, String> entry = System.getenv().entrySet().iterator().next();
        String firstEnvKey = entry.getKey();
        String firstEnvVal = entry.getValue();

        Assumptions.assumeFalse(firstEnvKey.isBlank());

        String val = ConsulM2MConfigDataLocationResolver.getPropsOrEnvsMust(args("not.exists"), args(firstEnvKey));
        assertEquals(firstEnvVal, val);
    }


    private static final String SECRET_ID = "test-secret-id";
    private static final String PROP_LOGIN_MODE = ConsulLoginProperties.PREFIX + ".mode";
    private static final String PROP_LOGIN_AUTH_METHOD = ConsulLoginProperties.PREFIX + ".auth-method";

    private final Map<String, Object> properties = new HashMap<>();
    private final AtomicInteger m2mLookups = new AtomicInteger();

    private DefaultBootstrapContext bootstrapContext;
    private String rejectedAuthMethod;
    private boolean clientReadsBearerToken;
    private ConfigDataLocationResolverContext resolverContext;
    private ConsulRestClient consulRestClient;

    private static class TestResolver extends ConsulM2MConfigDataLocationResolver {

        private final ConsulRestClient consulRestClient;

        TestResolver(DeferredLogFactory log, ConsulRestClient consulRestClient) {
            super(log);
            this.consulRestClient = consulRestClient;
        }

        @Override
        protected MicroserviceRestClient createMicroserviceRestClient() {
            return Mockito.mock(MicroserviceRestClient.class);
        }

        @Override
        protected ConsulRestClient createConsulRestClient(String consulAddr, Supplier<String> m2mTokenSupplier) {
            return consulRestClient;
        }
    }

    @BeforeEach
    void init() throws IOException {
        System.setProperty(PROP_CLOUD_NAMESPACE, "test-namespace");

        M2MManager m2MManager = Mockito.mock(M2MManager.class);
        Mockito.when(m2MManager.getToken()).thenReturn(Token.DUMMY_TOKEN);

        bootstrapContext = new DefaultBootstrapContext();
        bootstrapContext.register(ConsulProperties.class, BootstrapRegistry.InstanceSupplier.of(consulProperties()));
        bootstrapContext.register(M2MManager.class, context -> {
            m2mLookups.incrementAndGet();
            return m2MManager;
        });

        consulRestClient = Mockito.mock(ConsulRestClient.class);
        Mockito.when(consulRestClient.login(Mockito.any(ConsulLoginCredentials.class))).thenAnswer(invocation -> {
            ConsulLoginCredentials credentials = invocation.getArgument(0);
            if (rejectedAuthMethod != null && rejectedAuthMethod.equals(credentials.getAuthMethod())) {
                throw new IOException("consul auth method is not ready: response code=403; body='ACL not found'");
            }
            if (clientReadsBearerToken) {
                credentials.getBearerToken();
            }
            return new ConsulClientResponse("{\"SecretID\":\"" + SECRET_ID + "\"}", 200);
        });

        resolverContext = Mockito.mock(ConfigDataLocationResolverContext.class);
        Mockito.when(resolverContext.getBootstrapContext()).thenReturn(bootstrapContext);
        Mockito.when(resolverContext.getBinder())
                .thenAnswer(invocation -> new Binder(new MapConfigurationPropertySource(properties)));
    }

    @AfterEach
    void clearNamespace() {
        System.clearProperty(PROP_CLOUD_NAMESPACE);
    }

    private static ConsulProperties consulProperties() {
        ConsulProperties consulProperties = new ConsulProperties();
        consulProperties.setHost("consul");
        consulProperties.setPort(8500);
        return consulProperties;
    }

    private ConsulConfigProperties resolve() {
        return new TestResolver(Mockito.mock(DeferredLogFactory.class, Mockito.RETURNS_DEEP_STUBS), consulRestClient)
                .loadConfigProperties(resolverContext);
    }

    @Test
    void kubernetesModeWritesSecretIdWithoutTouchingTheBootstrapRegistry() {
        properties.put(PROP_LOGIN_MODE, "kubernetes");
        properties.put(PROP_LOGIN_AUTH_METHOD, "core-k8s");

        ConsulConfigProperties resolved = resolve();

        Assertions.assertEquals(SECRET_ID, resolved.getAclToken());
        Assertions.assertEquals(0, m2mLookups.get());
    }

    @Test
    void fallbackModeLeavesTheRegistryAloneWhileTheNewWayWorks() {
        properties.put(PROP_LOGIN_MODE, "kubernetes-with-m2m-fallback");
        properties.put(PROP_LOGIN_AUTH_METHOD, "core-k8s");

        ConsulConfigProperties resolved = resolve();

        Assertions.assertEquals(SECRET_ID, resolved.getAclToken());
        Assertions.assertEquals(0, m2mLookups.get());
    }

    @Test
    void fallbackModeAsksTheRegistryOnlyWhenBearerTokenIsNeeded() {
        properties.put(PROP_LOGIN_MODE, "kubernetes-with-m2m-fallback");
        properties.put(PROP_LOGIN_AUTH_METHOD, "core-k8s");
        rejectedAuthMethod = "core-k8s";
        clientReadsBearerToken = true;

        ConsulConfigProperties resolved = resolve();

        Assertions.assertEquals(SECRET_ID, resolved.getAclToken());
        Assertions.assertEquals(1, m2mLookups.get());
    }

    @Test
    void disabledM2MSkipsTheLoginAltogether() throws IOException {
        properties.put(PROP_CONSUL_M2M_ENABLED, "false");

        ConsulConfigProperties resolved = resolve();

        Assertions.assertNull(resolved.getAclToken());
        Assertions.assertEquals(0, m2mLookups.get());
        Mockito.verify(consulRestClient, Mockito.never()).login(Mockito.any(ConsulLoginCredentials.class));
    }

    @Test
    void m2mModeSkipsTheNewWayEntirely() {
        properties.put(PROP_LOGIN_MODE, "m2m");
        clientReadsBearerToken = true;

        ConsulConfigProperties resolved = resolve();

        Assertions.assertEquals(SECRET_ID, resolved.getAclToken());
        Assertions.assertEquals(1, m2mLookups.get());
    }

    @Test
    void aFailedLoginLeavesTheStartupAliveWithoutATokenInEveryMode() throws IOException {
        Mockito.when(consulRestClient.login(Mockito.any(ConsulLoginCredentials.class)))
                .thenThrow(new IOException("consul auth method is not ready: response code=403; body='ACL not found'"));

        for (String mode : new String[]{"kubernetes", "kubernetes-with-m2m-fallback", "m2m"}) {
            properties.put(PROP_LOGIN_MODE, mode);

            Assertions.assertNull(resolve().getAclToken(), mode);
        }
    }

    @Test
    void aMalformedAnswerEndsTheSameWayAsARejectedLogin() throws IOException {
        properties.put(PROP_LOGIN_MODE, "kubernetes");
        Mockito.when(consulRestClient.login(Mockito.any(ConsulLoginCredentials.class)))
                .thenReturn(new ConsulClientResponse("{\"NoSecretHere\":true}", 200));

        Assertions.assertNull(resolve().getAclToken());
    }

    @Test
    void unknownModeBreaksTheBinding() {
        properties.put(PROP_LOGIN_MODE, "cloud-foundry");

        Assertions.assertThrows(BindException.class, () ->
                new Binder(new MapConfigurationPropertySource(properties))
                        .bind(ConsulLoginProperties.PREFIX, ConsulLoginProperties.class));
    }
}

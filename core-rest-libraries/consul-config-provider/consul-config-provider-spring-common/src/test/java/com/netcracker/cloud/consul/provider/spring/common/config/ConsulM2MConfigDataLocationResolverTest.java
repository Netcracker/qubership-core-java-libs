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
import com.netcracker.cloud.security.core.utils.k8s.M2MAuthMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;
import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import static com.netcracker.cloud.consul.provider.spring.common.config.ConsulM2MConfigDataLocationResolver.ENV_CLOUD_NAMESPACE;
import static com.netcracker.cloud.consul.provider.spring.common.config.ConsulM2MConfigDataLocationResolver.ENV_NAMESPACE;
import static com.netcracker.cloud.consul.provider.spring.common.config.ConsulM2MConfigDataLocationResolver.PROP_CLOUD_NAMESPACE;
import static com.netcracker.cloud.consul.provider.spring.common.config.ConsulM2MConfigDataLocationResolver.PROP_CONSUL_M2M_ENABLED;

import static com.netcracker.cloud.consul.provider.spring.common.config.ConsulM2MConfigDataLocationResolver.args;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(SystemStubsExtension.class)
class ConsulM2MConfigDataLocationResolverTest {

    @SystemStub
    private EnvironmentVariables environmentVariables;

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
    private static final String PROP_AUTH_MODE = ConsulLoginProperties.PREFIX + ".mode";
    private static final String PROP_AUTH_METHOD = ConsulLoginProperties.PREFIX + ".method";

    private final Map<String, Object> properties = new HashMap<>();
    private final AtomicInteger m2mLookups = new AtomicInteger();

    private DefaultBootstrapContext bootstrapContext;
    private String rejectedAuthMethod;
    private boolean clientReadsBearerToken;
    private Supplier<M2MAuthMode> m2mAuthMode = () -> M2MAuthMode.LEGACY;
    private ConfigDataLocationResolverContext resolverContext;
    private ConsulRestClient consulRestClient;

    private static class TestResolver extends ConsulM2MConfigDataLocationResolver {

        private final ConsulRestClient consulRestClient;
        private final Supplier<M2MAuthMode> m2mAuthMode;

        TestResolver(DeferredLogFactory log, ConsulRestClient consulRestClient, Supplier<M2MAuthMode> m2mAuthMode) {
            super(log);
            this.consulRestClient = consulRestClient;
            this.m2mAuthMode = m2mAuthMode;
        }

        /** A {@code null} supplier leaves the read of the environment variable to the resolver itself. */
        @Override
        M2MAuthMode m2mAuthMode() {
            return m2mAuthMode == null ? super.m2mAuthMode() : m2mAuthMode.get();
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
                throw new IOException("consul refused the login (missing auth method, unmatched binding rule or wrong audience): response code=403; body='ACL not found'");
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
        return new TestResolver(Mockito.mock(DeferredLogFactory.class, Mockito.RETURNS_DEEP_STUBS), consulRestClient,
                m2mAuthMode)
                .loadConfigProperties(resolverContext);
    }

    @Test
    void k8sTakenFromM2MAuthModeNeedsNoNamespace() {
        environmentVariables.remove(ENV_NAMESPACE).remove(ENV_CLOUD_NAMESPACE);
        System.clearProperty(PROP_CLOUD_NAMESPACE);
        m2mAuthMode = () -> M2MAuthMode.K8S;

        ConsulConfigProperties resolved = resolve();

        Assertions.assertEquals(SECRET_ID, resolved.getAclToken());
        Assertions.assertEquals(0, m2mLookups.get());
    }

    @Test
    void m2mAuthModeIsReadFromTheEnvironmentVariable() throws IOException {
        environmentVariables.set(M2MAuthMode.M2M_AUTH_MODE_ENV, "k8s");
        m2mAuthMode = null;
        properties.put(PROP_AUTH_METHOD, "core-k8s");

        ConsulConfigProperties resolved = resolve();

        Assertions.assertEquals(SECRET_ID, resolved.getAclToken());
        ArgumentCaptor<ConsulLoginCredentials> used = ArgumentCaptor.forClass(ConsulLoginCredentials.class);
        Mockito.verify(consulRestClient).login(used.capture());
        Assertions.assertEquals("core-k8s", used.getValue().getAuthMethod());
    }

    @Test
    void consulAuthModeOverridesM2MAuthMode() {
        properties.put(PROP_AUTH_MODE, "legacy");
        m2mAuthMode = () -> M2MAuthMode.K8S;
        clientReadsBearerToken = true;

        ConsulConfigProperties resolved = resolve();

        Assertions.assertEquals(SECRET_ID, resolved.getAclToken());
        Assertions.assertEquals(1, m2mLookups.get());
    }

    @Test
    void unsupportedM2MAuthModeBreaksTheConfigDataPhaseWhenConsulAuthModeIsUnset() {
        m2mAuthMode = () -> {
            throw new IllegalArgumentException("M2M_AUTH_MODE has unsupported value \"cloud-foundry\"");
        };

        IllegalArgumentException thrown = Assertions.assertThrows(IllegalArgumentException.class, this::resolve);
        Assertions.assertTrue(thrown.getMessage().contains("consul.auth.mode is not set"), thrown.getMessage());
    }

    @Test
    void k8sModeWritesSecretIdWithoutTouchingTheBootstrapRegistry() {
        properties.put(PROP_AUTH_MODE, "k8s");
        properties.put(PROP_AUTH_METHOD, "core-k8s");

        ConsulConfigProperties resolved = resolve();

        Assertions.assertEquals(SECRET_ID, resolved.getAclToken());
        Assertions.assertEquals(0, m2mLookups.get());
    }

    @Test
    void hybridModeLeavesTheRegistryAloneWhileTheNewWayWorks() {
        properties.put(PROP_AUTH_MODE, "hybrid");
        properties.put(PROP_AUTH_METHOD, "core-k8s");

        ConsulConfigProperties resolved = resolve();

        Assertions.assertEquals(SECRET_ID, resolved.getAclToken());
        Assertions.assertEquals(0, m2mLookups.get());
    }

    @Test
    void hybridModeAsksTheRegistryOnlyWhenBearerTokenIsNeeded() {
        properties.put(PROP_AUTH_MODE, "hybrid");
        properties.put(PROP_AUTH_METHOD, "core-k8s");
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
    void legacyModeSkipsTheNewWayEntirely() {
        properties.put(PROP_AUTH_MODE, "legacy");
        clientReadsBearerToken = true;

        ConsulConfigProperties resolved = resolve();

        Assertions.assertEquals(SECRET_ID, resolved.getAclToken());
        Assertions.assertEquals(1, m2mLookups.get());
    }

    @Test
    void aFailedLoginLeavesTheStartupAliveWithoutATokenInEveryMode() throws IOException {
        Mockito.when(consulRestClient.login(Mockito.any(ConsulLoginCredentials.class)))
                .thenThrow(new IOException("consul refused the login (missing auth method, unmatched binding rule or wrong audience): response code=403; body='ACL not found'"));

        for (String mode : new String[]{"k8s", "hybrid", "legacy"}) {
            properties.put(PROP_AUTH_MODE, mode);

            Assertions.assertNull(resolve().getAclToken(), mode);
        }
    }

    @Test
    void aMalformedAnswerEndsTheSameWayAsARejectedLogin() throws IOException {
        properties.put(PROP_AUTH_MODE, "k8s");
        Mockito.when(consulRestClient.login(Mockito.any(ConsulLoginCredentials.class)))
                .thenReturn(new ConsulClientResponse("{\"NoSecretHere\":true}", 200));

        Assertions.assertNull(resolve().getAclToken());
    }

    @Test
    void unknownModeBreaksTheConfigDataPhase() {
        properties.put(PROP_AUTH_MODE, "cloud-foundry");

        Assertions.assertThrows(BindException.class, this::resolve);
    }

    @Test
    void aMissingNamespaceBreaksTheConfigDataPhase() {
        environmentVariables.remove(ENV_NAMESPACE).remove(ENV_CLOUD_NAMESPACE);
        System.clearProperty(PROP_CLOUD_NAMESPACE);
        properties.put(PROP_AUTH_MODE, "hybrid");

        Assertions.assertThrows(IllegalArgumentException.class, this::resolve);
    }

    @Test
    void unknownModeIsNotReadWhenTheExchangeIsOff() {
        properties.put(PROP_CONSUL_M2M_ENABLED, "false");
        properties.put(PROP_AUTH_MODE, "cloud-foundry");

        Assertions.assertNull(resolve().getAclToken());
    }
}

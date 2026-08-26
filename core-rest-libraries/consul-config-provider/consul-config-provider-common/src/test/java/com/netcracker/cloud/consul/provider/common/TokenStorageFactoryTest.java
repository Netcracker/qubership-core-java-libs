package com.netcracker.cloud.consul.provider.common;

import com.netcracker.cloud.consul.provider.common.client.ConsulClient;
import com.netcracker.cloud.consul.provider.common.client.ConsulClientResponse;
import com.netcracker.cloud.security.core.utils.k8s.AudienceName;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TokenStorageFactoryTest {

    private final String NAMESPACE = "ns";
    private final String CONSUL_URL = "http://consul.namespace:8500";

    @Test
    void returnsFine() {
        TokenStorageFactory.CreateOptions.Builder builder = new TokenStorageFactory.CreateOptions.Builder();
        Assertions.assertNotNull(builder);
        TokenStorageFactory.CreateOptions opts = builder.consulUrl(CONSUL_URL)
                .namespace(NAMESPACE)
                .m2mSupplier(() -> "token")
                .build();
        Assertions.assertNotNull(opts);
        Assertions.assertEquals(NAMESPACE, opts.namespace);
        Assertions.assertEquals(CONSUL_URL, opts.consulUrl);
    }

    @Test
    void cannotBuildWithoutUrlOrNamespaceOrM2MSupplier() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> new TokenStorageFactory.CreateOptions.Builder()
                .namespace(NAMESPACE)
                .m2mSupplier(() -> "token")
                .build());
        Assertions.assertThrows(IllegalArgumentException.class, () -> new TokenStorageFactory.CreateOptions.Builder()
                .consulUrl(CONSUL_URL)
                .m2mSupplier(() -> "token")
                .build());
        Assertions.assertThrows(IllegalArgumentException.class, () -> new TokenStorageFactory.CreateOptions.Builder()
                .consulUrl(CONSUL_URL)
                .namespace(NAMESPACE)
                .build());
    }

    @Test
    void modeIsAutoWhenNotGiven() {
        TokenStorageFactory.CreateOptions opts = new TokenStorageFactory.CreateOptions.Builder()
                .consulUrl(CONSUL_URL)
                .namespace(NAMESPACE)
                .m2mSupplier(() -> "token")
                .build();

        Assertions.assertEquals(ConsulLoginMode.AUTO, opts.mode);
    }

    @Test
    void autoModeNamesTheMissingInputAndTheMode() {
        IllegalArgumentException thrown = Assertions.assertThrows(IllegalArgumentException.class,
                () -> new TokenStorageFactory.CreateOptions.Builder()
                        .consulUrl(CONSUL_URL)
                        .mode(ConsulLoginMode.AUTO)
                        .build());

        Assertions.assertTrue(thrown.getMessage().contains("namespace"), thrown.getMessage());
        Assertions.assertTrue(thrown.getMessage().contains("auto"), thrown.getMessage());
    }

    @Test
    void kubernetesModeNeedsNeitherNamespaceNorM2MSupplier() {
        TokenStorageFactory.CreateOptions opts = new TokenStorageFactory.CreateOptions.Builder()
                .consulUrl(CONSUL_URL)
                .mode(ConsulLoginMode.KUBERNETES)
                .build();

        Assertions.assertEquals(ConsulLoginMode.KUBERNETES, opts.mode);
        Assertions.assertNull(opts.namespace);
        Assertions.assertNull(opts.m2mSupplier);
    }

    @Test
    void consulUrlIsRequiredInEveryMode() {
        for (ConsulLoginMode mode : ConsulLoginMode.values()) {
            Assertions.assertThrows(IllegalArgumentException.class, () -> new TokenStorageFactory.CreateOptions.Builder()
                    .mode(mode)
                    .namespace(NAMESPACE)
                    .m2mSupplier(() -> "token")
                    .build(), "mode " + mode);
        }
    }

    @Test
    void builderSuppliesAuthMethodAndAudienceDefaults() {
        TokenStorageFactory.CreateOptions opts = new TokenStorageFactory.CreateOptions.Builder()
                .consulUrl(CONSUL_URL)
                .mode(ConsulLoginMode.KUBERNETES)
                .build();

        Assertions.assertEquals(TokenStorageFactory.CreateOptions.DEFAULT_AUTH_METHOD, opts.authMethod);
        Assertions.assertEquals(AudienceName.NETCRACKER, opts.audience);
    }

    @Test
    void authMethodAndAudienceAreTakenFromTheCallerWhenGiven() {
        TokenStorageFactory.CreateOptions opts = new TokenStorageFactory.CreateOptions.Builder()
                .consulUrl(CONSUL_URL)
                .mode(ConsulLoginMode.KUBERNETES)
                .authMethod("core-k8s")
                .audience(AudienceName.DBAAS)
                .build();

        Assertions.assertEquals("core-k8s", opts.authMethod);
        Assertions.assertEquals(AudienceName.DBAAS, opts.audience);
    }

    @Test
    void unknownModeIsNotCreated() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> ConsulLoginMode.valueOf("cloud-foundry"));
    }

    private static ConsulLoginCredentials credentialsUsedBy(ConsulLogin login, ConsulClient client) throws IOException {
        when(client.login(any(ConsulLoginCredentials.class)))
                .thenReturn(new ConsulClientResponse("{\"SecretID\":\"test-secret-id\"}", 200));

        login.perform();

        ArgumentCaptor<ConsulLoginCredentials> used = ArgumentCaptor.forClass(ConsulLoginCredentials.class);
        verify(client).login(used.capture());
        return used.getValue();
    }

    @Test
    void m2mModeBuildsTokenProviderOverM2MCredentials() throws IOException {
        ConsulClient client = mock(ConsulClient.class);
        TokenStorageFactory.CreateOptions opts = new TokenStorageFactory.CreateOptions.Builder()
                .consulUrl(CONSUL_URL)
                .mode(ConsulLoginMode.M2M)
                .namespace(NAMESPACE)
                .m2mSupplier(() -> "token")
                .build();

        ConsulLogin login = TokenStorageFactory.from(client, opts);

        Assertions.assertInstanceOf(TokenProvider.class, login);
        Assertions.assertInstanceOf(M2MLoginCredentials.class, credentialsUsedBy(login, client));
    }

    @Test
    void kubernetesModeBuildsTokenProviderOverKubernetesCredentials() throws IOException {
        ConsulClient client = mock(ConsulClient.class);
        AtomicInteger m2mCalls = new AtomicInteger();
        TokenStorageFactory.CreateOptions opts = new TokenStorageFactory.CreateOptions.Builder()
                .consulUrl(CONSUL_URL)
                .mode(ConsulLoginMode.KUBERNETES)
                .authMethod("core-k8s")
                .m2mSupplier(() -> {
                    m2mCalls.incrementAndGet();
                    return "token";
                })
                .build();

        ConsulLogin login = TokenStorageFactory.from(client, opts);

        Assertions.assertInstanceOf(TokenProvider.class, login);
        ConsulLoginCredentials used = credentialsUsedBy(login, client);
        Assertions.assertInstanceOf(KubernetesLoginCredentials.class, used);
        Assertions.assertEquals("core-k8s", used.authMethod());
        Assertions.assertEquals(0, m2mCalls.get());
    }

    @Test
    void autoModeBuildsProbingLoginOverBothWays() throws IOException {
        ConsulClient client = mock(ConsulClient.class);
        TokenStorageFactory.CreateOptions opts = new TokenStorageFactory.CreateOptions.Builder()
                .consulUrl(CONSUL_URL)
                .mode(ConsulLoginMode.AUTO)
                .namespace(NAMESPACE)
                .authMethod("core-k8s")
                .m2mSupplier(() -> "token")
                .build();

        ConsulLogin login = TokenStorageFactory.from(client, opts);

        Assertions.assertInstanceOf(ProbingConsulLogin.class, login);
        Assertions.assertInstanceOf(KubernetesLoginCredentials.class, credentialsUsedBy(login, client));
    }

    @Test
    void autoModeFallsBackToM2MCredentials() throws IOException {
        ConsulClient client = mock(ConsulClient.class);
        TokenStorageFactory.CreateOptions opts = new TokenStorageFactory.CreateOptions.Builder()
                .consulUrl(CONSUL_URL)
                .mode(ConsulLoginMode.AUTO)
                .namespace(NAMESPACE)
                .m2mSupplier(() -> "token")
                .build();

        when(client.login(any(ConsulLoginCredentials.class))).thenAnswer(invocation -> {
            if (invocation.getArgument(0) instanceof KubernetesLoginCredentials) {
                throw new IOException("consul auth method is not ready: response code=403; body='ACL not found'");
            }
            return new ConsulClientResponse("{\"SecretID\":\"test-secret-id\"}", 200);
        });

        Token token = TokenStorageFactory.from(client, opts).perform();

        Assertions.assertEquals("test-secret-id", token.getSecretId());
        ArgumentCaptor<ConsulLoginCredentials> used = ArgumentCaptor.forClass(ConsulLoginCredentials.class);
        verify(client, atLeast(2)).login(used.capture());
        Assertions.assertInstanceOf(KubernetesLoginCredentials.class, used.getAllValues().get(0));
        Assertions.assertInstanceOf(M2MLoginCredentials.class, used.getAllValues().get(used.getAllValues().size() - 1));
    }
}

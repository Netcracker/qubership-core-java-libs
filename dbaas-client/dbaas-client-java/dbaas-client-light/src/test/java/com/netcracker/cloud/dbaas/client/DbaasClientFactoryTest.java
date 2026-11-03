package com.netcracker.cloud.dbaas.client;

import com.netcracker.cloud.dbaas.client.config.DbaasApiAddress;
import com.netcracker.cloud.security.core.auth.DummyM2MManager;
import com.netcracker.cloud.security.core.auth.M2MManager;
import com.netcracker.cloud.security.core.utils.k8s.M2MAuthMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.integration.ClientAndServer;

import java.util.ServiceLoader;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

class DbaasClientFactoryTest {

    private static final String PHYSICAL_DATABASES_URL = "/api/v3/dbaas/postgresql/physical_databases";

    private ClientAndServer mockServer;
    private String dbaasUrl;

    @BeforeEach
    void setUp() {
        mockServer = startClientAndServer();
        dbaasUrl = String.format("http://localhost:%d", mockServer.getLocalPort());
        mockServer.when(request().withPath(PHYSICAL_DATABASES_URL))
                .respond(response().withStatusCode(200).withBody("{\"identified\":{}}"));
    }

    @AfterEach
    void tearDown() {
        mockServer.stop();
    }

    @Test
    void authorizesRequestsOnItsOwn() {
        DbaasClient client = DbaasClientFactory.builder()
                .mode(M2MAuthMode.LEGACY)
                .agentAddress(dbaasUrl)
                .legacyToken(() -> "legacy-token")
                .build();

        assertNotNull(client.getPhysicalDatabases("postgresql"));

        mockServer.verify(request().withPath(PHYSICAL_DATABASES_URL).withHeader("Authorization", "Bearer legacy-token"));
    }

    @Test
    void keepsInterceptorsOfItsOwn() {
        DbaasClient client = DbaasClientFactory.builder()
                .mode(M2MAuthMode.LEGACY)
                .agentAddress(dbaasUrl)
                .legacyToken(() -> "legacy-token")
                .httpClient(builder -> builder.addInterceptor(chain ->
                        chain.proceed(chain.request().newBuilder().header("x-own", "header").build())))
                .build();

        assertNotNull(client.getPhysicalDatabases("postgresql"));

        mockServer.verify(request().withPath(PHYSICAL_DATABASES_URL).withHeader("x-own", "header"));
    }

    @Test
    void k8sModeRequiresDbaasAddress() {
        assumeTrue(System.getenv(DbaasApiAddress.DBAAS_ADDRESS_ENV) == null);

        assertThrows(IllegalStateException.class, () -> DbaasClientFactory.builder()
                .mode(M2MAuthMode.K8S)
                .agentAddress(dbaasUrl)
                .build());
    }

    @Test
    void legacyModeFallsBackToTheDummyToken() {
        assumeTrue(ServiceLoader.load(M2MManager.class).stream()
                .map(ServiceLoader.Provider::get)
                .allMatch(DummyM2MManager.class::isInstance));

        DbaasClient client = DbaasClientFactory.builder()
                .mode(M2MAuthMode.LEGACY)
                .agentAddress(dbaasUrl)
                .build();

        assertNotNull(client.getPhysicalDatabases("postgresql"));

        mockServer.verify(request().withPath(PHYSICAL_DATABASES_URL).withHeader("Authorization", "Bearer dummy"));
    }
}

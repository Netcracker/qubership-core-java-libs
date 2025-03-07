package org.qubership.cloud.maas.client.impl.apiversion;

import org.qubership.cloud.maas.client.Utils;
import org.qubership.cloud.maas.client.impl.http.HttpClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.junit.jupiter.MockServerExtension;
import org.mockserver.matchers.Times;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

@ExtendWith(MockServerExtension.class)
class ServerApiVersionTest {

    @Test
    void testV1LegacySupport(ClientAndServer mockServer) {
        var serverApiVersion = setup(mockServer, "v1");
        assertTrue(serverApiVersion.isCompatible(2,12));
        assertFalse(serverApiVersion.isCompatible(2,14));
    }

    @Test
    void testV1_5MigrationSupport(ClientAndServer mockServer) {
        var serverApiVersion = setup(mockServer, "v1.5");
        assertTrue(serverApiVersion.isCompatible(2,14));
    }

    @Test
    void testV2Support(ClientAndServer mockServer) {
        var serverApiVersion = setup(mockServer, "v2");
        assertTrue(serverApiVersion.isCompatible(2,12));
        assertFalse(serverApiVersion.isCompatible(2,14));

        assertThrows(RuntimeException.class, () -> serverApiVersion.requiresApiVersion(3, 0));
    }

    private ServerApiVersion setup(ClientAndServer mockServer, String version) {
        mockServer.when(
                request()
                        .withPath("/api-version")
                        .withMethod("GET")
                        .withHeader("authorization", "Bearer faketoken"),
                Times.once()
        ).respond(
                response()
                        .withStatusCode(200)
                        .withBody(Utils.readResourceAsString("api-version." + version + ".json"))
        );

        var httpClient = new HttpClient(() -> "faketoken");
        return new ServerApiVersion(httpClient, "http://localhost:" + mockServer.getPort());
    }
}
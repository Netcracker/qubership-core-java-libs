package com.netcracker.cloud.security.core.utils.k8s;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@ExtendWith(MockitoExtension.class)
class CloudProviderTest {

    private HttpServer server;
    private String baseUrl;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
        resetCache();
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
        resetCache();
    }

    @Test
    void detect_returnsGke_whenMetadataFlavorIsGoogle() {
        server.createContext("/computeMetadata/v1/", exchange -> {
            exchange.getResponseHeaders().add("Metadata-Flavor", "Google");
            exchange.sendResponseHeaders(200, 0);
            exchange.getResponseBody().close();
        });
        stubNotFound("/latest/meta-data/");
        stubNotFound("/metadata/instance");

        assertEquals(CloudProvider.GKE, CloudProvider.detect(baseUrl));
    }

    @Test
    void detect_notGke_whenMetadataFlavorHeaderAbsent() {
        server.createContext("/computeMetadata/v1/", exchange -> {
            exchange.sendResponseHeaders(200, 0);
            exchange.getResponseBody().close();
        });
        stubNotFound("/latest/meta-data/");
        stubNotFound("/metadata/instance");

        assertEquals(CloudProvider.ON_PREM, CloudProvider.detect(baseUrl));
    }

    @Test
    void detect_returnsEks_whenImdsV1Returns200() {
        stubNotFound("/computeMetadata/v1/");
        server.createContext("/latest/meta-data/", exchange -> {
            exchange.sendResponseHeaders(200, 0);
            exchange.getResponseBody().close();
        });
        stubNotFound("/metadata/instance");

        assertEquals(CloudProvider.EKS, CloudProvider.detect(baseUrl));
    }

    @Test
    void detect_returnsEks_whenImdsV2Returns401() {
        stubNotFound("/computeMetadata/v1/");
        server.createContext("/latest/meta-data/", exchange -> {
            exchange.sendResponseHeaders(401, 0);
            exchange.getResponseBody().close();
        });
        stubNotFound("/metadata/instance");

        assertEquals(CloudProvider.EKS, CloudProvider.detect(baseUrl));
    }

    @Test
    void detect_notEks_whenImdsReturns403() {
        stubNotFound("/computeMetadata/v1/");
        server.createContext("/latest/meta-data/", exchange -> {
            exchange.sendResponseHeaders(403, 0);
            exchange.getResponseBody().close();
        });
        stubNotFound("/metadata/instance");

        assertEquals(CloudProvider.ON_PREM, CloudProvider.detect(baseUrl));
    }

    @Test
    void detect_returnsAks_whenAzureInstanceEndpointReturns200() {
        stubNotFound("/computeMetadata/v1/");
        stubNotFound("/latest/meta-data/");
        server.createContext("/metadata/instance", exchange -> {
            byte[] body = "{\"compute\":{\"azEnvironment\":\"AzurePublicCloud\"}}".getBytes();
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });

        assertEquals(CloudProvider.AKS, CloudProvider.detect(baseUrl));
    }

    @Test
    void detect_notAks_whenAzureEndpointReturns404() {
        stubNotFound("/computeMetadata/v1/");
        stubNotFound("/latest/meta-data/");
        stubNotFound("/metadata/instance");

        assertEquals(CloudProvider.ON_PREM, CloudProvider.detect(baseUrl));
    }

    @Test
    void detect_returnsOnPrem_whenMetadataUnreachable() {
        assertEquals(CloudProvider.ON_PREM, CloudProvider.detect("http://127.0.0.1:19999"));
    }

    @Test
    void detect_returnsOnPrem_whenAllEndpointsReturn500() {
        stubStatus("/computeMetadata/v1/", 500);
        stubStatus("/latest/meta-data/", 500);
        stubStatus("/metadata/instance", 500);

        assertEquals(CloudProvider.ON_PREM, CloudProvider.detect(baseUrl));
    }

    @Test
    void detect_prefersGke_whenBothGkeAndEksProbesSucceed() {
        server.createContext("/computeMetadata/v1/", exchange -> {
            exchange.getResponseHeaders().add("Metadata-Flavor", "Google");
            exchange.sendResponseHeaders(200, 0);
            exchange.getResponseBody().close();
        });
        server.createContext("/latest/meta-data/", exchange -> {
            exchange.sendResponseHeaders(200, 0);
            exchange.getResponseBody().close();
        });
        stubNotFound("/metadata/instance");

        assertEquals(CloudProvider.GKE, CloudProvider.detect(baseUrl));
    }

    @Test
    void getCloudProvider_isUncomputed_beforeFirstDetection() {
        assertNull(readCache());
    }

    @Test
    void getCloudProvider_returnsAndKeepsCachedValue_onceDetected() {
        seedCache(CloudProvider.AKS);

        assertEquals(CloudProvider.AKS, CloudProvider.getCloudProvider());
        assertEquals(CloudProvider.AKS, CloudProvider.getCloudProvider());
    }

    private void stubNotFound(String path) {
        server.createContext(path, exchange -> {
            exchange.sendResponseHeaders(404, 0);
            exchange.getResponseBody().close();
        });
    }

    private void stubStatus(String path, int status) {
        server.createContext(path, exchange -> {
            exchange.sendResponseHeaders(status, 0);
            exchange.getResponseBody().close();
        });
    }

    private static void resetCache() {
        CloudProvider.detected = null;
    }

    private static void seedCache(CloudProvider value) {
        CloudProvider.detected = value;
    }

    private static CloudProvider readCache() {
        return CloudProvider.detected;
    }
}

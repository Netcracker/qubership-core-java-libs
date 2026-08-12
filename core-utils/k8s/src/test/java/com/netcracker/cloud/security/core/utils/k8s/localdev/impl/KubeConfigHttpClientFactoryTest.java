package com.netcracker.cloud.security.core.utils.k8s.localdev.impl;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class KubeConfigHttpClientFactoryTest {

    @TempDir
    Path tempDir;

    private final KubeConfigHttpClientFactory factory = new KubeConfigHttpClientFactory();

    @Test
    void createInsecureForLocalDevBuildsHttpClient() {
        HttpClient client = factory.createInsecureForLocalDev();
        assertNotNull(client);
    }

    @Test
    void createWithInsecureSkipTlsVerify() {
        KubeConfigCredentials credentials = KubeConfigCredentials.builder()
                .serverUrl("https://api.example")
                .userToken("token")
                .insecureSkipTlsVerify(true)
                .build();
        assertNotNull(factory.create(credentials));
    }

    @Test
    void createWithEmptyCaUsesDefaultSslContext() {
        KubeConfigCredentials credentials = KubeConfigCredentials.builder()
                .serverUrl("https://api.example")
                .userToken("token")
                .certificateAuthorityData(new byte[0])
                .build();
        assertNotNull(factory.create(credentials));
    }

    @Test
    void createWithInvalidCaThrows() {
        KubeConfigCredentials credentials = KubeConfigCredentials.builder()
                .serverUrl("https://api.example")
                .userToken("token")
                .certificateAuthorityData("not-a-certificate".getBytes(StandardCharsets.UTF_8))
                .build();
        assertThrows(IllegalStateException.class, () -> factory.create(credentials));
    }

    @Test
    void createInsecureForLocalDevAcceptsSelfSignedHttps() throws Exception {
        MockWebServer server = new MockWebServer();
        server.start();
        try {
            server.enqueue(new MockResponse().setBody("ok"));
            HttpClient client = factory.createInsecureForLocalDev();
            HttpRequest request = HttpRequest.newBuilder(server.url("/").uri()).GET().build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            assertEquals(200, response.statusCode());
            assertEquals("ok", response.body());
        } finally {
            server.shutdown();
        }
    }

    @Test
    void createWithValidCaFromKeytool() throws Exception {
        Path keystore = tempDir.resolve("test.p12");
        Path certFile = tempDir.resolve("ca.pem");
        runKeytool(new String[]{
                "-genkeypair", "-alias", "test", "-keyalg", "RSA",
                "-storetype", "PKCS12", "-keystore", keystore.toString(),
                "-storepass", "changeit", "-keypass", "changeit",
                "-dname", "CN=local-dev-test", "-validity", "1"
        });
        runKeytool(new String[]{
                "-exportcert", "-alias", "test", "-keystore", keystore.toString(),
                "-storepass", "changeit", "-rfc", "-file", certFile.toString()
        });

        byte[] caPem = Files.readAllBytes(certFile);
        KubeConfigCredentials credentials = KubeConfigCredentials.builder()
                .serverUrl("https://api.example")
                .userToken("token")
                .certificateAuthorityData(caPem)
                .build();
        assertNotNull(factory.create(credentials));
    }

    private static void runKeytool(String[] args) throws Exception {
        String javaHome = System.getProperty("java.home");
        Path keytool = Path.of(javaHome, "bin", "keytool");
        if (!Files.isRegularFile(keytool)) {
            keytool = Path.of(javaHome, "bin", "keytool.exe");
        }
        String[] command = new String[args.length + 1];
        command[0] = keytool.toString();
        System.arraycopy(args, 0, command, 1, args.length);
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        boolean finished = process.waitFor(30, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new IllegalStateException("keytool timed out");
        }
        if (process.exitValue() != 0) {
            throw new IllegalStateException("keytool failed with exit code " + process.exitValue());
        }
    }
}

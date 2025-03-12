package org.qubership.cloud.bluegreen.quarkus.config;


import org.qubership.cloud.bluegreen.impl.http.HttpClientAdapter;
import org.qubership.cloud.consul.provider.common.TokenStorage;
import jakarta.enterprise.context.ApplicationScoped;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.consul.ConsulContainer;
import org.testcontainers.containers.Container.ExecResult;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Testcontainers
public abstract class AbstractConsulTest {

    static String ns1 = "test-namespace-1";
    static String ns2 = "test-namespace-2";

    static List<String> namespaces = List.of(ns1, ns2);

    static Pattern bootstrapTokenPattern = Pattern.compile("SecretID:\\s+(.+)");
    static String bootstrapToken;
    static String consulUrl;
    static HttpClientAdapter client;

    static ConsulContainer consulContainer = new ConsulContainer("hashicorp/consul:1.16")
            .withCopyToContainer(MountableFile.forClasspathResource("consul-acl.json"),
                    "/consul/config/consul-acl.json");

    @BeforeAll
    static void before() throws Exception {
        consulContainer.start();
        consulUrl = String.format("http://%s:%d", consulContainer.getHost(), consulContainer.getMappedPort(8500));
        System.setProperty("consul.url", consulUrl);
        ExecResult execResult = consulContainer.execInContainer("/bin/sh", "-c", "consul acl bootstrap");
        Assertions.assertEquals(0, execResult.getExitCode());
        bootstrapToken = Arrays.stream(execResult.getStdout().split("\n")).map(l -> bootstrapTokenPattern.matcher(l))
                .filter(Matcher::matches)
                .map(m -> m.group(1)).findFirst().orElseThrow(() -> new IllegalStateException("Failed to get bootstrap tooken"));
        Assertions.assertNotNull(bootstrapToken);
        client = new HttpClientAdapter(() -> bootstrapToken);
    }

    @AfterAll
    static void cleanup() {
        consulContainer.stop();
    }

    @ApplicationScoped
    static class TestTokenStorage implements TokenStorage {

        @Override
        public String get() {
            return bootstrapToken;
        }

        @Override
        public void update(String s) {
        }
    }
}

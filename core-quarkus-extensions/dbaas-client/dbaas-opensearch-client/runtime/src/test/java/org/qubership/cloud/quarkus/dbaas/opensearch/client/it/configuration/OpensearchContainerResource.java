package org.qubership.cloud.quarkus.dbaas.opensearch.client.it.configuration;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.Map;

public class OpensearchContainerResource implements QuarkusTestResourceLifecycleManager {

    private static final DockerImageName OPENSEARCH_IMAGE = DockerImageName.parse("opensearchproject/opensearch:1.2.4");

    public static final int OPENSEARCH_PORT = 9200;

    public static GenericContainer opensearchContainer;

    @Override
    public Map<String, String> start() {
        opensearchContainer = new GenericContainer(OPENSEARCH_IMAGE)
                .withExposedPorts(OPENSEARCH_PORT)
                .withEnv("DISABLE_SECURITY_PLUGIN", "true")
                .withEnv("DISABLE_INSTALL_DEMO_CONFIG", "true")
                .withEnv("discovery.type", "single-node")
                .withStartupTimeout(Duration.ofSeconds(120));
        opensearchContainer.start();
        return null;
    }

    @Override
    public void stop() {
        opensearchContainer.stop();
    }
}

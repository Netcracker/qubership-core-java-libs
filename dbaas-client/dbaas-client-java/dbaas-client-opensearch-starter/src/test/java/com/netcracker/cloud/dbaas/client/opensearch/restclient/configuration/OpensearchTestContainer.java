package com.netcracker.cloud.dbaas.client.opensearch.restclient.configuration;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.TestcontainersConfiguration;

import java.time.Duration;

@Slf4j
public class OpensearchTestContainer extends GenericContainer<OpensearchTestContainer> {
    private static final DockerImageName OPENSEARCH_IMAGE = DockerImageName.parse("opensearchproject/opensearch:3.5.0");

    public static final int OPENSEARCH_PORT = 9200;
    private static OpensearchTestContainer opensearchContainer;

    static final String RYUK_CONTAINER_IMAGE_PROPERTY = "ryuk.container.image";
    static final String RYUK_CONTAINER_IMAGE = "testcontainers/ryuk:0.11.0";

    static {
        TestcontainersConfiguration configs = TestcontainersConfiguration.getInstance();
        configs.updateUserConfig(RYUK_CONTAINER_IMAGE_PROPERTY, RYUK_CONTAINER_IMAGE);
    }

    private OpensearchTestContainer() {
        super(OPENSEARCH_IMAGE);
    }

    @PreDestroy
    public void destroy(){
        opensearchContainer.stop();
    }


    public static OpensearchTestContainer getInstance() {
        if (opensearchContainer == null) {
            opensearchContainer = new OpensearchTestContainer()
                    .withExposedPorts(OPENSEARCH_PORT)
                    .withEnv("DISABLE_SECURITY_PLUGIN", "true")
                    .withEnv("DISABLE_INSTALL_DEMO_CONFIG", "true")
                    .withEnv("discovery.type", "single-node")
                    .withStartupTimeout(Duration.ofSeconds(120));
        }
        return opensearchContainer;
    }
}

package com.netcracker.cloud.dbaas.client.it.cassandra.config;

import lombok.extern.slf4j.Slf4j;
import org.testcontainers.containers.CassandraContainer;
import org.testcontainers.utility.TestcontainersConfiguration;

import java.time.Duration;

@Slf4j
public class CassandraTestContainer extends CassandraContainer<CassandraTestContainer> {
    private static final String IMAGE_VERSION = "cassandra:3";
    private static CassandraTestContainer container;

    private CassandraTestContainer() {
        super(IMAGE_VERSION);
    }

    static final String RYUK_CONTAINER_IMAGE_PROPERTY = "ryuk.container.image";
    static final String RYUK_CONTAINER_IMAGE = "testcontainers/ryuk:0.11.0";

    static {
        TestcontainersConfiguration configs = TestcontainersConfiguration.getInstance();
        configs.updateUserConfig(RYUK_CONTAINER_IMAGE_PROPERTY, RYUK_CONTAINER_IMAGE);
    }

    public static CassandraTestContainer getInstance() {
        if (container == null) {
            container = new CassandraTestContainer()
                    .withExposedPorts(9042)
                    .withStartupTimeout(Duration.ofSeconds(240));
        }
        return container;
    }

    @Override
    public void start() {
        super.start();
    }

    @Override
    public void stop() {
        super.stop();
    }
}

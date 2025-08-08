package com.netcracker.cloud.dbaas.client.it.postgresql;

import lombok.extern.slf4j.Slf4j;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.TestcontainersConfiguration;

import java.time.Duration;

@Slf4j
public class PostgresqlContainerConfiguration extends PostgreSQLContainer<PostgresqlContainerConfiguration> {
    private static final String IMAGE_VERSION = "postgres:15.6";
    private static PostgresqlContainerConfiguration container;

    private PostgresqlContainerConfiguration() {
        super(IMAGE_VERSION);
    }

    static final String RYUK_CONTAINER_IMAGE_PROPERTY = "ryuk.container.image";
    static final String RYUK_CONTAINER_IMAGE = "testcontainers/ryuk:0.11.0";

    static {
        TestcontainersConfiguration configs = TestcontainersConfiguration.getInstance();
        configs.updateUserConfig(RYUK_CONTAINER_IMAGE_PROPERTY, RYUK_CONTAINER_IMAGE);
    }

    public static PostgresqlContainerConfiguration getInstance() {
        if (container == null) {
            container = new PostgresqlContainerConfiguration().withUsername("root")
                    .withPassword("password")
                    .withInitScript("init_test_container_databases.sql")
                    .withStartupTimeout(Duration.ofSeconds(120));
        }
        return container;
    }

    @Override
    public void stop() {
        super.stop();
        container = null;
    }
}

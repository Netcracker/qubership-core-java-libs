package org.qubership.cloud.dbaas.client;

import lombok.extern.slf4j.Slf4j;
import org.testcontainers.containers.PostgreSQLContainer;

import java.time.Duration;

@Slf4j
public class PostgresqlContainerConfiguration extends PostgreSQLContainer<PostgresqlContainerConfiguration> {
    private static final String IMAGE_VERSION = "postgres:15.6";
    private static PostgresqlContainerConfiguration container;

    private PostgresqlContainerConfiguration() {
        super(IMAGE_VERSION);
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
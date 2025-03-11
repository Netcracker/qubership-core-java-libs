package org.qubership.cloud.core.quarkus.dbaas.datasource.testcontainers.configuration;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

import org.apache.commons.lang3.ArrayUtils;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.Map;

public class PostgresqlContainerResource implements QuarkusTestResourceLifecycleManager {

    public static final String IMAGE_ENV_KEY = "POSTGRESQL_IMAGE";

    public static final String ADMIN_PASSWORD = "postgres";

    public static final String ADMIN_USERNAME = "postgres";

    public static final String ADMIN_DB = "postgres";

    public static PostgreSQLContainer postgresql;


    @Override
    public Map<String, String> start() {
        postgresql = new PostgreSQLContainer<>(DockerImageName.parse(
                System.getenv().getOrDefault(IMAGE_ENV_KEY, "postgres:14.5")))
                .withUsername(ADMIN_USERNAME)
                .withPassword(ADMIN_PASSWORD)
                .withDatabaseName(ADMIN_DB);
        String[] commandParts = postgresql.getCommandParts();
        commandParts = ArrayUtils.addAll(commandParts, "-c", "max_prepared_transactions=100", "-c", "max_connections=100");
        postgresql.setCommandParts(commandParts);

        postgresql.start();
        return null;
    }

    @Override
    public void stop() {
        postgresql.stop();
    }
}

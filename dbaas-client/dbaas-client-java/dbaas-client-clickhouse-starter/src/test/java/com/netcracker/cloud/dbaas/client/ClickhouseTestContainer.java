package com.netcracker.cloud.dbaas.client;

import lombok.extern.slf4j.Slf4j;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

@Slf4j
public class ClickhouseTestContainer extends GenericContainer<ClickhouseTestContainer> {
    private static final String IMAGE_VERSION = "clickhouse/clickhouse-server:24";
    public static final String CLICKHOUSE_ADMIN_PWD = "admin";
    public static final String CLICKHOUSE_ADMIN_USERNAME = "admin";
    public static final String CLICKHOUSE_ADMIN_DB = "admin";
    public static final int CLICKHOUSE_PORT = 8123;

    private static GenericContainer<?> container;

    private ClickhouseTestContainer() {
        super(IMAGE_VERSION);
    }

    public static GenericContainer<?> getInstance() {
        if (container == null) {
            container = new GenericContainer<>(IMAGE_VERSION)
                    .withEnv("CLICKHOUSE_USER", CLICKHOUSE_ADMIN_USERNAME)
                    .withEnv("CLICKHOUSE_PASSWORD", CLICKHOUSE_ADMIN_PWD)
                    .withEnv("CLICKHOUSE_DB", CLICKHOUSE_ADMIN_DB)
                    .withExposedPorts(CLICKHOUSE_PORT)
                    .waitingFor(Wait.forHttp("/ping").forPort(CLICKHOUSE_PORT).forStatusCode(200));
        }
        return container;
    }

    @Override
    public void stop() {
        super.stop();
        container = null;
    }
}

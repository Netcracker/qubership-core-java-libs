package org.qubership.cloud.dbaas.client;

import lombok.extern.slf4j.Slf4j;
import org.testcontainers.clickhouse.ClickHouseContainer;
import org.testcontainers.containers.GenericContainer;

@Slf4j
public class ClickhouseTestContainer extends GenericContainer<ClickhouseTestContainer> {
    private static final String IMAGE_VERSION = "clickhouse/clickhouse-server:24";
    public static final String CLICKHOUSE_ADMIN_PWD = "admin";
    public static final String CLICKHOUSE_ADMIN_USERNAME = "admin";
    public static final String CLICKHOUSE_ADMIN_DB = "admin";
    public static final int CLICKHOUSE_PORT = 8123;

    private static ClickHouseContainer container;

    private ClickhouseTestContainer() {
        super(IMAGE_VERSION);
    }

    public static ClickHouseContainer getInstance() {
        if (container == null) {
            container = new ClickHouseContainer(IMAGE_VERSION)
                    .withUsername(CLICKHOUSE_ADMIN_USERNAME)
                    .withPassword(CLICKHOUSE_ADMIN_PWD)
                    .withDatabaseName(CLICKHOUSE_ADMIN_DB);
        }
        return container;
    }

    @Override
    public void stop() {
        super.stop();
        container = null;
    }
}

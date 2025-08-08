package com.netcracker.cloud.dbaas.client.config.container;

import lombok.extern.slf4j.Slf4j;

import org.testcontainers.containers.CassandraContainer;

import java.time.Duration;

@Slf4j
public class CassandraTestContainer extends CassandraContainer<CassandraTestContainer> {
    private static final String IMAGE_VERSION = "cassandra:latest";
    private static CassandraTestContainer container;

    private CassandraTestContainer() {
        super(IMAGE_VERSION);
    }

    public static CassandraTestContainer getInstance() {
        if (container == null) {
            container = new CassandraTestContainer()
                    .withInitScript("init_script.cql")
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

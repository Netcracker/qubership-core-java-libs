package com.netcracker.cloud.microserviceframework.testconfig;

import lombok.extern.slf4j.Slf4j;
import org.testcontainers.containers.GenericContainer;

import java.time.Duration;

import static org.qubership.cloud.microserviceframework.testconfig.TestConstants.MONGO_DB_NAME;

@Slf4j
public class MongoTestContainer extends GenericContainer<MongoTestContainer> {
    private static final String IMAGE_VERSION = "mongo:4.0.10";
    private static final int MONGO_PORT = 27017;

    private static MongoTestContainer container;

    private MongoTestContainer() {
        super(IMAGE_VERSION);
    }

    public static MongoTestContainer getInstance() {
        if (container == null) {
            container = new MongoTestContainer()
                    .withEnv("MONGO_INITDB_DATABASE", MONGO_DB_NAME)
                    .withExposedPorts(MONGO_PORT)
                    .withStartupTimeout(Duration.ofSeconds(120));
        }
        return container;
    }

    public int getPort() {
        return container.getMappedPort(MONGO_PORT);
    }

    @Override
    public void stop() {
        super.stop();
        container = null;
    }
}

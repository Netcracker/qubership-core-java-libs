package org.qubership.cloud.dbaas.client.it.mongodb;

import lombok.extern.slf4j.Slf4j;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.TestcontainersConfiguration;

import java.time.Duration;

@Slf4j
public class MongoTestContainer extends GenericContainer<MongoTestContainer> {
    private static final String IMAGE_VERSION = "mongo:7.0";
    public static final String MONGO_ADMIN_PWD = "admin";
    public static final String MONGO_ADMIN_USERNAME = "admin";
    public static final String MONGO_ADMIN_DB = "admin";
    public static final int MONGO_PORT = 27017;

    private static MongoTestContainer container;

    static final String RYUK_CONTAINER_IMAGE_PROPERTY = "ryuk.container.image";
    static final String RYUK_CONTAINER_IMAGE = "testcontainers/ryuk:0.11.0";

    static {
        TestcontainersConfiguration configs = TestcontainersConfiguration.getInstance();
        configs.updateUserConfig(RYUK_CONTAINER_IMAGE_PROPERTY, RYUK_CONTAINER_IMAGE);
    }

    private MongoTestContainer() {
        super(IMAGE_VERSION);
    }

    public static MongoTestContainer getInstance() {
        if (container == null) {
            container = new MongoTestContainer()
                    .withEnv("MONGO_INITDB_ROOT_USERNAME", MONGO_ADMIN_USERNAME)
                    .withEnv("MONGO_INITDB_ROOT_PASSWORD", MONGO_ADMIN_PWD)
                    .withEnv("MONGO_INITDB_DATABASE", MONGO_ADMIN_DB)
                    .withExposedPorts(MONGO_PORT)
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

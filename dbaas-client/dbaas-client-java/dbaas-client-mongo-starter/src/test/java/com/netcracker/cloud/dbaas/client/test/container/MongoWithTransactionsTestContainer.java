package com.netcracker.cloud.dbaas.client.test.container;

import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.utility.TestcontainersConfiguration;

import java.time.Duration;

import static org.qubership.cloud.dbaas.client.test.container.MongoTestContainer.IMAGE_VERSION;
import static org.qubership.cloud.dbaas.client.test.container.MongoTestContainer.MONGO_ADMIN_DB;
import static org.qubership.cloud.dbaas.client.test.container.MongoTestContainer.MONGO_PORT;
import static org.qubership.cloud.dbaas.client.test.container.MongoTestContainer.RYUK_CONTAINER_IMAGE;
import static org.qubership.cloud.dbaas.client.test.container.MongoTestContainer.RYUK_CONTAINER_IMAGE_PROPERTY;

/**
 * Test container for MongoDB with configured replica set (for support transactions)
 * and without root user (MongoTemplate should not contain credentials)
 */
public class MongoWithTransactionsTestContainer extends MongoDBContainer {

    private static MongoWithTransactionsTestContainer container;

    private MongoWithTransactionsTestContainer() {
        super(IMAGE_VERSION);
    }

    static {
        TestcontainersConfiguration configs = TestcontainersConfiguration.getInstance();
        configs.updateUserConfig(RYUK_CONTAINER_IMAGE_PROPERTY, RYUK_CONTAINER_IMAGE);
    }

    public static MongoWithTransactionsTestContainer getInstance() {
        if (container == null) {
            container = (MongoWithTransactionsTestContainer) new MongoWithTransactionsTestContainer()
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

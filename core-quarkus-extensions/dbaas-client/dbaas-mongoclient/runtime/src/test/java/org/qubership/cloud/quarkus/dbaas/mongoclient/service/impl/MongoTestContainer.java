package org.qubership.cloud.quarkus.dbaas.mongoclient.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.testcontainers.containers.GenericContainer;

import java.time.Duration;

@Slf4j
public class MongoTestContainer extends GenericContainer<MongoTestContainer> {
    private static final String IMAGE_VERSION = "mongo:7.0";
    public static final String MONGO_ADMIN_USERNAME = "adminUsr";
    public static final String MONGO_ADMIN_PWD = "adminPwd";
    public static final String MONGO_ADMIN_DB = "admin";
    public static final int MONGO_PORT = 27017;

    private static MongoTestContainer container;

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

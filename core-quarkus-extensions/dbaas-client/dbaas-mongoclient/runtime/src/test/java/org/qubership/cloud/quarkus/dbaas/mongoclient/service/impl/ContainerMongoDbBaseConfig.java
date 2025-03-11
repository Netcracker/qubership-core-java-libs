package org.qubership.cloud.quarkus.dbaas.mongoclient.service.impl;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.checkerframework.checker.signature.qual.Identifier;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import java.util.UUID;

import static org.qubership.cloud.quarkus.dbaas.mongoclient.service.impl.MongoTestContainer.*;

public class ContainerMongoDbBaseConfig {
    private static MongoTestContainer container;

    protected static final String USERNAME = MONGO_ADMIN_USERNAME;
    protected static final String PASSWORD = MONGO_ADMIN_PWD;
    protected static final String DATABASE = MONGO_ADMIN_DB;
    protected static String URL = "";

    @BeforeAll
    protected static void setUp() {
        container = MongoTestContainer.getInstance();
        container.start();
        URL = "mongodb://" + container.getHost() + ":" + container.getMappedPort(MONGO_PORT) + "/" + DATABASE;
    }

    @AfterAll
    protected static void tearDown() {
        if (container.isRunning()) {
            container.stop();
        }
    }

    @Data
    @AllArgsConstructor
    public static class TestEntityWithStringId {
        @Identifier
        private String id;

        private String name;
    }

    @Data
    @AllArgsConstructor
    public static class TestEntityWithUUID {
        @Identifier
        private UUID id;

        private String name;
    }
}

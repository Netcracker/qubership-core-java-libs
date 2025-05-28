package org.qubership.cloud.mongoevolution.java.annotation;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.qubership.cloud.mongoevolution.java.MongoServerConfiguration;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@Slf4j
class DBManagerEntityTest extends MongoServerConfiguration {

    private DBManagerEntity dbManagerEntity;

    public final static String TEST_DB_NAME = "test";

    @BeforeEach
    void prepareTestContext() {
        dbManagerEntity = new DBManagerEntity();
    }

    @Test
    void testGetSetMongoDB() {
        assertNull(dbManagerEntity.getMongoDatabase());
        dbManagerEntity.setMongoDatabase(mongoClient.getDatabase(TEST_DB_NAME));
        assertNotNull(dbManagerEntity.getMongoDatabase());
    }

    @Test
    void testGetSetMongoDatabase() {
        assertNull(dbManagerEntity.getMongoDatabase());
        dbManagerEntity.setMongoDatabase(mongoClient.getDatabase(TEST_DB_NAME));
        assertNotNull(dbManagerEntity.getMongoDatabase());
    }
}

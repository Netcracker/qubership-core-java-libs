package org.qubership.cloud.mongoevolution.java.annotation;

import org.qubership.cloud.mongoevolution.java.MongoServerConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

@Slf4j
public class DBManagerEntityTest extends MongoServerConfiguration {

    private DBManagerEntity dbManagerEntity;

    public final static String TEST_DB_NAME = "test";

    @Before
    public void prepareTestContext(){
        dbManagerEntity = new DBManagerEntity();
    }

    @Test
    public void testGetSetMongoDB() {
        Assert.assertNull(dbManagerEntity.getMongoDatabase());
        dbManagerEntity.setMongoDatabase(mongoClient.getDatabase(TEST_DB_NAME));
        Assert.assertNotNull(dbManagerEntity.getMongoDatabase());
    }

    @Test
    public void testGetSetMongoDatabase() {
        Assert.assertNull(dbManagerEntity.getMongoDatabase());
        dbManagerEntity.setMongoDatabase(mongoClient.getDatabase(TEST_DB_NAME));
        Assert.assertNotNull(dbManagerEntity.getMongoDatabase());
    }

}
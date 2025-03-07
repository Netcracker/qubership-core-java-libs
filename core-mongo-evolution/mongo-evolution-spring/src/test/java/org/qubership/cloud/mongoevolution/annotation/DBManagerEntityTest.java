package org.qubership.cloud.mongoevolution.annotation;

import com.mongodb.client.MongoClient;
import org.qubership.cloud.mongoevolution.MongoTestConfiguration;
import org.qubership.cloud.mongoevolution.SpringDBManagerEntity;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import static org.qubership.cloud.mongoevolution.MongoTestConfiguration.TEST_DB_NAME;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = MongoTestConfiguration.class)
@Slf4j
public class DBManagerEntityTest {

    @Autowired
    private SpringDBManagerEntity dbManagerEntity;
    @Autowired
    private MongoClient mongoClient;
    @Autowired
    private MongoTemplate mongoTemplate;

    @Test
    public void testGetSetMongoDatabase() {
        Assert.assertNull(dbManagerEntity.getMongoDatabase());
        dbManagerEntity.setMongoDatabase(mongoClient.getDatabase(TEST_DB_NAME));
        Assert.assertNotNull(dbManagerEntity.getMongoDatabase());
    }

    @Test
    public void testGetSetMongoTemplate() {
        Assert.assertNull(dbManagerEntity.getMongoTemplate());
        dbManagerEntity.setMongoTemplate(mongoTemplate);
        Assert.assertNotNull(dbManagerEntity.getMongoTemplate());
    }

}
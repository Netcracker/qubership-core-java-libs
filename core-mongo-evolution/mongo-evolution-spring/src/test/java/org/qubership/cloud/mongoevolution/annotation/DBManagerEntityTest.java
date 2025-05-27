package org.qubership.cloud.mongoevolution.annotation;

import com.mongodb.client.MongoClient;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.qubership.cloud.mongoevolution.MongoTestConfiguration;
import org.qubership.cloud.mongoevolution.SpringDBManagerEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.qubership.cloud.mongoevolution.MongoTestConfiguration.TEST_DB_NAME;

@SpringBootTest(classes = MongoTestConfiguration.class)
@Slf4j
public class DBManagerEntityTest {

    @Autowired
    private SpringDBManagerEntity dbManagerEntity;
    @Autowired
    private MongoClient mongoClient;
    @Autowired
    private MongoTemplate mongoTemplate;

    @Test
    void testGetSetMongoDatabase() {
        assertNull(dbManagerEntity.getMongoDatabase());
        dbManagerEntity.setMongoDatabase(mongoClient.getDatabase(TEST_DB_NAME));
        assertNotNull(dbManagerEntity.getMongoDatabase());
    }

    @Test
    void testGetSetMongoTemplate() {
        assertNull(dbManagerEntity.getMongoTemplate());
        dbManagerEntity.setMongoTemplate(mongoTemplate);
        assertNotNull(dbManagerEntity.getMongoTemplate());
    }
}

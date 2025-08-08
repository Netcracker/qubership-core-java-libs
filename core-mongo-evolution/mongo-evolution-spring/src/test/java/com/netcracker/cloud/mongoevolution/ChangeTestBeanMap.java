package com.netcracker.cloud.mongoevolution;

import com.mongodb.client.MongoCollection;
import lombok.extern.apachecommons.CommonsLog;
import org.bson.Document;
import org.qubership.cloud.mongoevolution.java.annotation.ChangeLog;
import org.qubership.cloud.mongoevolution.java.annotation.ChangeSet;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.Map;

@CommonsLog
@ChangeLog(version = 3)
public class ChangeTestBeanMap {

    @ChangeSet
    public void someChange(MongoTemplate mongoTemplate, Map<String, Object> beans) {
        log.debug("@ChangeSet MongoTemplate.class with beans");
        MongoCollection<Document> collection = mongoTemplate.getCollection(TestConstants.COLLECTION_NAME);
        Document doc = new Document().append(TestConstants.COLUMN_NAME, TestConstants.COLUMN_VALUE + mongoTemplate.getClass().getSimpleName());
        collection.insertOne(doc);
    }
}

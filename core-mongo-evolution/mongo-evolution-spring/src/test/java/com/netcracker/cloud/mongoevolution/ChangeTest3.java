package com.netcracker.cloud.mongoevolution;

import com.mongodb.client.MongoCollection;
import org.qubership.cloud.mongoevolution.java.annotation.ChangeLog;
import org.qubership.cloud.mongoevolution.java.annotation.ChangeSet;
import lombok.extern.apachecommons.CommonsLog;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;

@CommonsLog
@ChangeLog(version = 3)
public class ChangeTest3 {

    @ChangeSet(order=2)
    public void someChange() {
        log.debug("@ChangeSet Empty args");
    }

    @ChangeSet
    public void someChange(MongoTemplate mongoTemplate) {
        log.debug("@ChangeSet MongoTemplate.class");
        MongoCollection<Document> collection = mongoTemplate.getCollection(TestConstants.COLLECTION_NAME);
        Document doc = new Document().append(TestConstants.COLUMN_NAME, TestConstants.COLUMN_VALUE + mongoTemplate.getClass().getSimpleName());
        collection.insertOne(doc);
    }
}

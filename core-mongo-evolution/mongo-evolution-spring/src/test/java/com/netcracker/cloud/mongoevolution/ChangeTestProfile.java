package com.netcracker.cloud.mongoevolution;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.qubership.cloud.mongoevolution.java.annotation.ChangeLog;
import org.qubership.cloud.mongoevolution.java.annotation.ChangeSet;
import lombok.extern.apachecommons.CommonsLog;
import org.bson.Document;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@CommonsLog
@ChangeLog(version = 10, order = 1, dbClassifier = TestConstants.DEFAULT_DB_NAME)
@Profile(TestConstants.TEST_PROFILE)
public class ChangeTestProfile {

    @ChangeSet(order = 2)
    public void someChange() {
        log.debug("@ChangeSet Empty args");
    }

    @ChangeSet(order = 1)
    public void someChange(MongoDatabase db) {
        log.debug("@ChangeSet MongoDatabase.class");
        MongoCollection collection = db.getCollection(TestConstants.COLLECTION_NAME);
        Document doc = new Document().append(TestConstants.COLUMN_NAME, TestConstants.COLUMN_VALUE);
        collection.insertOne(doc);
    }

    @ChangeSet(order = 3)
    public void findDocumentUsingMongoTemplate(MongoTemplate mongoTemplate) {
        log.debug("@ChangeSet find document using mongoTemplate");
        long count = mongoTemplate.count(Query.query(Criteria.where(TestConstants.COLUMN_NAME).is(TestConstants.COLUMN_VALUE)), TestConstants.COLLECTION_NAME);
        assertEquals(1, count);
    }

    @ChangeSet(order = 4)
    public void updateDocumentUsingMongoTemplate(MongoTemplate mongoTemplate) {
        log.debug("@ChangeSet update document using mongoTemplate");
        String columnValueUpdate = "update";
        mongoTemplate.updateFirst(Query.query(
                Criteria.where(TestConstants.COLUMN_NAME).is(TestConstants.COLUMN_VALUE)),
                Update.update(TestConstants.COLUMN_NAME, columnValueUpdate), TestConstants.COLLECTION_NAME);

        List<TestObject> testObjects = mongoTemplate.find(Query.query(Criteria.where(TestConstants.COLUMN_NAME).is(columnValueUpdate)), TestObject.class, TestConstants.COLLECTION_NAME);
        assertEquals(1, testObjects.size());
        assertEquals(columnValueUpdate, testObjects.get(0).getName());

    }


}

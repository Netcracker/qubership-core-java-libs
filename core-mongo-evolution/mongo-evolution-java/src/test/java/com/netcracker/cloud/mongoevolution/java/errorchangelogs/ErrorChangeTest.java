package com.netcracker.cloud.mongoevolution.java.errorchangelogs;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.netcracker.cloud.mongoevolution.java.TestConstants;
import com.netcracker.cloud.mongoevolution.java.annotation.ChangeLog;
import com.netcracker.cloud.mongoevolution.java.annotation.ChangeSet;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;


@Slf4j
@ChangeLog(version = 1, order = 1, dbClassifier = TestConstants.ERROR_DEFAULT_DB_NAME)
public class ErrorChangeTest {
    @ChangeSet(order = 1)
    public void someChange(MongoDatabase db) {
        log.debug("@ChangeSet first method");
        MongoCollection collection = db.getCollection(TestConstants.COLLECTION_NAME);
        Document doc = new Document().append(TestConstants.COLUMN_NAME, TestConstants.COLUMN_VALUE);
        collection.insertOne(doc);
    }

    @ChangeSet(order = 2)
    public void otherChange(MongoDatabase db) {
        log.debug("@ChangeSet second method");
        throw new RuntimeException("error");
    }

    @ChangeSet(order = 3)
    public void lastChange(MongoDatabase db) {
        log.debug("@ChangeSet third method");
        MongoCollection collection = db.getCollection(TestConstants.COLLECTION_NAME);
        Document doc = new Document().append(TestConstants.COLUMN_NAME, TestConstants.COLUMN_VALUE);
        collection.insertOne(doc);
    }
}


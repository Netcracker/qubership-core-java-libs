package com.netcracker.cloud.mongoevolution.java.basechangelogs;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.netcracker.cloud.mongoevolution.java.TestConstants;
import com.netcracker.cloud.mongoevolution.java.annotation.ChangeLog;
import com.netcracker.cloud.mongoevolution.java.annotation.ChangeSet;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;

@Slf4j
@ChangeLog(version = 1, order = 1, dbClassifier = TestConstants.DEFAULT_DB_NAME)
public class BaseChangeTest {
    @ChangeSet(order = 1)
    public void someChange(MongoDatabase db) {
        log.debug("@ChangeSet baseChangeTest");
        MongoCollection collection = db.getCollection(TestConstants.PACKAGE_COLLECTION_NAME);
        Document doc = new Document().append(TestConstants.COLUMN_NAME, TestConstants.COLUMN_VALUE);
        collection.insertOne(doc);
    }
}

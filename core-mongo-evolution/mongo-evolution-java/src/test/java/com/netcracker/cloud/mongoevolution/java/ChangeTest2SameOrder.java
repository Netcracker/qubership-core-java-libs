package com.netcracker.cloud.mongoevolution.java;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.qubership.cloud.mongoevolution.java.annotation.ChangeLog;
import org.qubership.cloud.mongoevolution.java.annotation.ChangeSet;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;

@Slf4j
@ChangeLog(version = 2)
public class ChangeTest2SameOrder {

    @ChangeSet(order = 2)
    public void someChange(MongoDatabase db) {
        log.debug("@ChangeSet MongoDatabase.class");
        MongoCollection collection = db.getCollection(TestConstants.COLLECTION_NAME);
        Document doc = new Document().append(TestConstants.COLUMN_NAME, TestConstants.COLUMN_VALUE + db.getClass().getSimpleName());
        collection.insertOne(doc);
    }


}

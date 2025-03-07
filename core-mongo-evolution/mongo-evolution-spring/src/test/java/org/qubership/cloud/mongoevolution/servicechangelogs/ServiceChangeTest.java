package org.qubership.cloud.mongoevolution.servicechangelogs;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.qubership.cloud.mongoevolution.TestConstants;
import org.qubership.cloud.mongoevolution.java.annotation.ChangeLog;
import org.qubership.cloud.mongoevolution.java.annotation.ChangeSet;
import lombok.extern.apachecommons.CommonsLog;
import org.bson.Document;

@CommonsLog
@ChangeLog(version = 2, order = 1, dbClassifier = TestConstants.DEFAULT_DB_NAME)
public class ServiceChangeTest {
    @ChangeSet(order = 1)
    public void someChange(MongoDatabase db) {
        log.debug("@ChangeSet serviceChangeTest");
        MongoCollection collection = db.getCollection(TestConstants.PACKAGE_COLLECTION_NAME);
        Document doc = new Document().append(TestConstants.COLUMN_NAME, TestConstants.COLUMN_VALUE);
        collection.insertOne(doc);
    }
}

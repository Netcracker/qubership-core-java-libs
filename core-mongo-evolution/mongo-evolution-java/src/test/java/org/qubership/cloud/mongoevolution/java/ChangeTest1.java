package org.qubership.cloud.mongoevolution.java;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.qubership.cloud.mongoevolution.java.annotation.ChangeLog;
import org.qubership.cloud.mongoevolution.java.annotation.ChangeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Slf4j
@ChangeLog(version = 1, order = 1, dbClassifier = TestConstants.DEFAULT_DB_NAME)
public class ChangeTest1 {

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
    public void findDocumentUsingMongoDatabase(MongoDatabase mongoDatabase) {
        log.debug("@ChangeSet find document");
        MongoCollection<TestObject> collection = mongoDatabase.getCollection(TestConstants.COLLECTION_NAME, TestObject.class);
        long count = collection.countDocuments(Filters.eq(TestConstants.COLUMN_NAME, TestConstants.COLUMN_VALUE));
        assertEquals(1, count);
    }

    @ChangeSet(order = 4)
    public void updateDocumentUsingMongoDatabase(MongoDatabase mongoDatabase) {
        log.debug("@ChangeSet update document using mongoDatabase");
        MongoCollection<TestObject> collection = mongoDatabase.getCollection(TestConstants.COLLECTION_NAME, TestObject.class);
        Bson filterObject = Filters.eq(TestConstants.COLUMN_NAME, TestConstants.COLUMN_VALUE);
        Bson updated = Updates.set("name", TestConstants.COLUMN_VALUE_UPDATE);
        assertNotNull(collection.updateOne(filterObject, updated));

        assertEquals(1, collection.countDocuments());

        MongoCursor<TestObject> iterator = collection.find(Filters.eq(TestConstants.COLUMN_NAME, TestConstants.COLUMN_VALUE_UPDATE)).iterator();

        assertEquals(TestConstants.COLUMN_VALUE_UPDATE, iterator.next().getName());

    }

}

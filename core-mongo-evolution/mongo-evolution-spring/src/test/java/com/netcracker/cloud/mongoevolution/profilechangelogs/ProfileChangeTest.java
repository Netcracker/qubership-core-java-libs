package org.qubership.cloud.mongoevolution.profilechangelogs;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.qubership.cloud.mongoevolution.TestConstants;
import org.qubership.cloud.mongoevolution.java.annotation.ChangeLog;
import org.qubership.cloud.mongoevolution.java.annotation.ChangeSet;
import lombok.extern.apachecommons.CommonsLog;
import org.bson.Document;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.AbstractEnvironment;

import java.util.Map;

@CommonsLog
@ChangeLog(version = 3, order = 1, dbClassifier = TestConstants.DEFAULT_DB_NAME)
@Profile(TestConstants.TEST_PROFILE_2)
public class ProfileChangeTest {
    @ChangeSet(order = 1)
    public void someChange(MongoDatabase db, Map<String, Object> beans) {
        log.debug("@ChangeSet profileChangeTest");
        MongoCollection collection = db.getCollection(TestConstants.PACKAGE_COLLECTION_NAME);
        Document doc = new Document().append(TestConstants.COLUMN_NAME, TestConstants.COLUMN_VALUE);
        collection.insertOne(doc);

    }
}

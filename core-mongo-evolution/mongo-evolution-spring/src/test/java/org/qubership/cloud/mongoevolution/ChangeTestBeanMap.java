package org.qubership.cloud.mongoevolution;

import com.mongodb.client.MongoCollection;
import org.qubership.cloud.mongoevolution.java.annotation.ChangeLog;
import org.qubership.cloud.mongoevolution.java.annotation.ChangeSet;
import lombok.extern.apachecommons.CommonsLog;
import org.bson.Document;
import org.springframework.core.env.AbstractEnvironment;
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

        AbstractEnvironment env = (AbstractEnvironment) beans.get("org.springframework.core.env.StandardEnvironment");
        if(null == env || env.getSystemEnvironment().isEmpty()) {
            log.error("AnnotationProcessor or MongoDbSchemaEvolution should be created with Beans Map");
            System.exit(1);
        }
    }
}

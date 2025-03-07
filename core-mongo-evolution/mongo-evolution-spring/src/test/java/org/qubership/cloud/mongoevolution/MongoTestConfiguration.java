package org.qubership.cloud.mongoevolution;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import de.bwaldvogel.mongo.MongoServer;
import de.bwaldvogel.mongo.backend.memory.MemoryBackend;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;

@Configuration
public class MongoTestConfiguration {

    public final static String TEST_DB_NAME = "test";
    public final static String DB_USER = "dbaas";
    public final static String DB_PASSWORD = "dbaas";
    public final static String MONGO_TEST_URI = "mongodb://"
            + DB_USER + ":" + DB_PASSWORD + "@localhost:27017,localhost:27018/";

    @Bean
    public MongoTemplate mongoTemplate() {
        return new MongoTemplate(mongoDbFactory());
    }

    @Bean
    public MongoDatabaseFactory mongoDbFactory() {
        com.mongodb.client.MongoClient mongoClient = MongoClients.create(MONGO_TEST_URI);
        return new SimpleMongoClientDatabaseFactory(mongoClient, TEST_DB_NAME);
    }

    @Bean(destroyMethod = "shutdown")
    public MongoServer mongoServer() {
        MongoServer mongoServer = new MongoServer(new MemoryBackend());
        mongoServer.bind();
        return mongoServer;
    }

    @Bean(destroyMethod = "close")
    public MongoClient mongoClient(MongoServer mongoServer) {
        mongoServer.bind();
        return MongoClients.create("mongodb:/" + mongoServer.getLocalAddress());
    }

    @Bean
    public SpringDBManagerEntity dbManagerEntity() {
        return new SpringDBManagerEntity();
    }
}
package org.qubership.cloud.microserviceframework.testconfig;

import org.qubership.cloud.dbaas.client.entity.connection.MongoDBConnection;
import org.qubership.cloud.dbaas.client.entity.database.MongoDatabase;
import jakarta.annotation.PreDestroy;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.util.TreeMap;

import static org.qubership.cloud.microserviceframework.testconfig.TestConstants.MONGO_DB_NAME;

@TestConfiguration
public class MongoTestConfiguration {

    private MongoTestContainer container;

    @Bean
    public MongoTestContainer mongoTestContainer() {
        container = MongoTestContainer.getInstance();
        container.start();
        return container;
    }

    @PreDestroy
    public void close() {
        if (container.isRunning()) {
            container.stop();
        }
    }

    @Bean
    public MongoDatabase mongoDatabase(MongoTestContainer container) {
        MongoDatabase database = new MongoDatabase();
        database.setName(MONGO_DB_NAME);
        MongoDBConnection connection = new MongoDBConnection();
        connection.setUrl("mongodb://" + container.getHost() + ":" + container.getPort() + "/" + MONGO_DB_NAME);
        database.setConnectionProperties(connection);
        database.setClassifier(new TreeMap<>());
        return database;
    }
}

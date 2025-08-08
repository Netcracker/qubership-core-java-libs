package com.netcracker.cloud.dbaas.client.it.mongodb;

import com.netcracker.cloud.dbaas.client.DbaasClient;
import com.netcracker.cloud.dbaas.client.entity.connection.MongoDBConnection;
import com.netcracker.cloud.dbaas.client.entity.database.MongoDatabase;
import com.netcracker.cloud.dbaas.client.management.DatabaseConfig;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.util.HashMap;

import static com.netcracker.cloud.dbaas.client.DbaasConst.*;
import static com.netcracker.cloud.dbaas.client.it.mongodb.MongoTestContainer.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@TestConfiguration
public class MongoTestConfiguration {

    @Autowired
    @Qualifier("mongoContainer")
    MongoTestContainer container;

    @Bean
    @Primary
    public DbaasClient testDbaasClient() {
        DbaasClient dbaasClient = Mockito.mock(DbaasClient.class);
        when(dbaasClient.getOrCreateDatabase(any(), any(), any(), any(DatabaseConfig.class)))
                .thenAnswer((Answer<MongoDatabase>) invocationOnMock -> {
                    HashMap<String, String> classifierFromMock = (HashMap<String, String>) invocationOnMock.getArguments()[2];
                    String databaseName = classifierFromMock.get(SCOPE).equals(SERVICE) ? "service-test" : classifierFromMock.get(TENANT_ID);
                    return getMongoDatabase(databaseName);
                });

        return dbaasClient;
    }

    public MongoDatabase getMongoDatabase(String dbName) {
        MongoDatabase database = new MongoDatabase();
        database.setName(dbName);
        MongoDBConnection connection = new MongoDBConnection();
        connection.setUrl("mongodb://" + container.getHost() + ":" + container.getMappedPort(MONGO_PORT) + "/" + dbName);
        connection.setPassword(MONGO_ADMIN_PWD);
        connection.setUsername(MONGO_ADMIN_USERNAME);
        connection.setAuthDbName(MONGO_ADMIN_DB);
        database.setConnectionProperties(connection);
        return database;
    }
}

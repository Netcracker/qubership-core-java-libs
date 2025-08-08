package com.netcracker.cloud.dbaas.client.test.configuration;

import com.netcracker.cloud.dbaas.client.DbaasClient;
import com.netcracker.cloud.dbaas.client.test.PersonService;
import com.netcracker.cloud.dbaas.client.config.EnableDbaasMongo;
import com.netcracker.cloud.dbaas.client.entity.connection.MongoDBConnection;
import com.netcracker.cloud.dbaas.client.entity.database.MongoDatabase;
import com.netcracker.cloud.dbaas.client.management.DatabaseConfig;
import com.netcracker.cloud.dbaas.client.test.container.MongoTestContainer;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import java.util.Map;

import static com.netcracker.cloud.dbaas.client.DbaasConst.*;
import static com.netcracker.cloud.dbaas.client.test.container.MongoTestContainer.*;
import static com.netcracker.cloud.dbaas.client.config.DbaasMongoConfiguration.SERVICE_MONGO_TEMPLATE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@Configuration
@EnableDbaasMongo
@EnableMongoRepositories(
        basePackages = "com.netcracker.cloud.dbaas.client",
        mongoTemplateRef = SERVICE_MONGO_TEMPLATE)
public class TestMongoRepositoriesConfiguration {
    @Bean
    public PersonService personService() {
        return new PersonService();
    }

    @Autowired
    @Qualifier("mongoContainer")
    MongoTestContainer container;

    @Bean
    @Primary
    public DbaasClient dbaasClient() {
        DbaasClient dbaasClient = Mockito.mock(DbaasClient.class);
        when(dbaasClient.getOrCreateDatabase(any(), any(), any(), any(DatabaseConfig.class)))
                .thenAnswer((Answer<MongoDatabase>) invocationOnMock -> {
                    Map<String, String> classifierFromMock = (Map<String, String>) invocationOnMock.getArguments()[2];
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

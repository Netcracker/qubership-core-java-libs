package org.qubership.cloud.dbaas.client.test.configuration;

import org.qubership.cloud.dbaas.client.DbaasClient;
import org.qubership.cloud.dbaas.client.config.DefaultMSInfoProvider;
import org.qubership.cloud.dbaas.client.config.EnableDbaasMongo;
import org.qubership.cloud.dbaas.client.config.MSInfoProvider;
import org.qubership.cloud.dbaas.client.entity.connection.MongoDBConnection;
import org.qubership.cloud.dbaas.client.entity.database.MongoDatabase;
import org.qubership.cloud.dbaas.client.entity.database.type.MongoDBType;
import org.qubership.cloud.dbaas.client.management.DatabaseConfig;
import org.qubership.cloud.restclient.MicroserviceRestClient;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@Configuration
@EnableDbaasMongo
public class TestMongoConfiguration {

    public static String mongoTestHost = "localhost";
    public static int mongoTestPort = 27017;
    public static String mongoTestDbName = "authdb";


    @Bean
    @Primary
    public DbaasClient testDbaasClient() {
        DbaasClient dbaasClient = Mockito.mock(DbaasClient.class);
        when(dbaasClient.getOrCreateDatabase(any(MongoDBType.class), anyString(), anyMap(), any(DatabaseConfig.class)))
                .thenReturn(getMongoDatabase());
        return dbaasClient;
    }

    public MongoDatabase getMongoDatabase() {
        MongoDatabase database = new MongoDatabase();
        database.setName(mongoTestDbName);
        MongoDBConnection connection = new MongoDBConnection();
        connection.setUrl("mongodb://mongo:mongo@" +mongoTestHost + ":" +mongoTestPort +"/" + mongoTestDbName);
        connection.setUsername("mongo");
        connection.setPassword("mongo");
        database.setConnectionProperties(connection);
        return database;
    }

    @Bean
    public MSInfoProvider msInfoProvider() {
        return new DefaultMSInfoProvider();
    }

    @Primary
    @Bean
    @Qualifier("dbaasRestClient")
    public static MicroserviceRestClient microserviceRestClient() {
        return Mockito.mock(MicroserviceRestClient.class);
    }

}

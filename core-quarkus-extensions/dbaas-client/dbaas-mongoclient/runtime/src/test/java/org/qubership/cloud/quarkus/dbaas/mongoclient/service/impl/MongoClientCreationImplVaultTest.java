package org.qubership.cloud.quarkus.dbaas.mongoclient.service.impl;

import org.qubership.cloud.dbaas.client.DbaaSClientOkHttpImpl;
import org.qubership.cloud.dbaas.client.DbaasClient;
import org.qubership.cloud.dbaas.client.entity.DbaasApiProperties;
import org.qubership.cloud.dbaas.client.management.DatabaseConfig;
import org.qubership.cloud.dbaas.common.config.DbaasApiPropertiesConfig;
import org.qubership.cloud.quarkus.dbaas.mongoclient.config.properties.DbaasMongoDbCreationConfig;
import org.qubership.cloud.quarkus.dbaas.mongoclient.entity.connection.MongoDBConnection;
import org.junit.jupiter.api.BeforeAll;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MongoClientCreationImplVaultTest extends ContainerMongoDbBaseConfig {

    private static MongoClientCreationImpl mongoClientCreationImpl;
    private static final MongoDBConnection mongoDBConnection = new MongoDBConnection();

    @BeforeAll
    public static void createDb() {
        DbaasMongoDbCreationConfig dbaasMongoDbCreationConfig = new DbaasMongoDbCreationConfig();
        DbaasApiPropertiesConfig dbaasApiPropertiesConfig = mock(DbaasApiPropertiesConfig.class);
        when(dbaasApiPropertiesConfig.getDbaaseApiProperties()).thenReturn(new DbaasApiProperties());
        dbaasMongoDbCreationConfig.dbaasApiPropertiesConfig = dbaasApiPropertiesConfig;

        mongoClientCreationImpl = new MongoClientCreationImpl(dbaasMongoDbCreationConfig);
        mongoDBConnection.setUsername(USERNAME);
        mongoDBConnection.setUrl(URL);
        mongoDBConnection.setAuthDbName(DATABASE);

        mongoClientCreationImpl.namespace = "test-namespace";
        org.qubership.cloud.quarkus.dbaas.mongoclient.entity.database.MongoDatabase mongoDatabase =
                new org.qubership.cloud.quarkus.dbaas.mongoclient.entity.database.MongoDatabase();
        mongoDatabase.setConnectionProperties(mongoDBConnection);
        mongoDatabase.setName(DATABASE);

        DbaasClient dbaaSClient = mock(DbaaSClientOkHttpImpl.class);
        when(dbaaSClient.getOrCreateDatabase(any(), anyString(), anyMap(), any(DatabaseConfig.class))).thenReturn(mongoDatabase);
        mongoClientCreationImpl.dbaaSClient = dbaaSClient;
    }
}

package com.netcracker.cloud.dbaas.client.arangodb.test.configuration;

import com.arangodb.serde.ArangoSerde;
import com.arangodb.springframework.core.convert.ArangoConverter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.netcracker.cloud.dbaas.client.DbaasClient;
import com.netcracker.cloud.dbaas.client.arangodb.configuration.EnableServiceDbaasArangoDB;
import com.netcracker.cloud.dbaas.client.arangodb.entity.Person;
import com.netcracker.cloud.dbaas.client.arangodb.entity.connection.ArangoConnection;
import com.netcracker.cloud.dbaas.client.arangodb.entity.database.ArangoDatabase;
import com.netcracker.cloud.dbaas.client.management.DatabaseConfig;
import lombok.extern.slf4j.Slf4j;

import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.mapping.MappingException;

import java.io.IOException;
import java.util.HashMap;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static com.netcracker.cloud.dbaas.client.arangodb.test.ArangoTestCommon.*;

@EnableServiceDbaasArangoDB
@Configuration
@Slf4j
public class TestArangoDBConfiguration {

    public static final String DB_NAME_1 = "db-test-name-1";
    public static final String DB_NAME_2 = "db-test-name-2";

    @Bean
    public TestArangoDBContainer getContainer() {
        TestArangoDBContainer container = TestArangoDBContainer.getInstance();
        container.start();
        return container;
    }

    @Bean
    @Primary
    public ArangoSerde ArangoSerdeConfiguration(ArangoConverter converter) {

        ArangoSerde arangoSerde = new ArangoSerde() {
            private final ObjectMapper mapper;

            {
                mapper = new ObjectMapper();
                SimpleModule module = new SimpleModule("PersonModule");
                module.addDeserializer(Person.class, new PersonDeserializer());
                mapper.registerModule(module);
            }


            @Override
            public byte[] serialize(Object value) {
                try {
                    return mapper.writeValueAsBytes(converter.write(value));
                } catch (JsonProcessingException e) {
                    throw new MappingException("Exception while serializing.", e);
                }
            }

            @Override
            public <T> T deserialize(byte[] content, Class<T> clazz) {
                try {
                    return converter.read(clazz, mapper.readTree(content));
                } catch (IOException e) {
                    throw new MappingException("Exception while deserializing.", e);
                }
            }
        };

        return arangoSerde;
    }

    @Bean
    @Primary
    public DbaasClient dbaasClient(TestArangoDBContainer container) {
        DbaasClient dbaasClient = Mockito.mock(DbaasClient.class);
        when(dbaasClient.getOrCreateDatabase(any(), any(), any(), any(DatabaseConfig.class)))
                .thenAnswer((Answer<ArangoDatabase>) invocationOnMock -> {
                    HashMap<String, String> classifierFromMock = (HashMap<String, String>) invocationOnMock.getArguments()[2];
                    String dbId = classifierFromMock.get("dbId");
                    return getArangoDatabase(dbId, container);
                });
        return dbaasClient;
    }

    public ArangoDatabase getArangoDatabase(String dbName, TestArangoDBContainer container) {
        ArangoDatabase arangoDatabase = new ArangoDatabase();
        arangoDatabase.setName(dbName);

        ArangoConnection connection = new ArangoConnection();
        String host = container.getHost();
        Integer port = container.getMappedPort(DB_PORT);
        connection.setDbName(dbName);
        connection.setHost(host);
        connection.setPort(port);
        connection.setUsername(TEST_USER);
        connection.setPassword(TEST_PASSWORD);

        arangoDatabase.setConnectionProperties(connection);
        log.debug("Returning ArangoDatabase from DbaasClient. ArangoConnection: {}.", connection);
        return arangoDatabase;
    }
}

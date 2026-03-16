package com.netcracker.cloud.dbaas.client.arangodb.management;

import com.arangodb.ArangoDB;
import com.arangodb.internal.config.ArangoConfig;
import com.arangodb.serde.ArangoSerde;
import com.netcracker.cloud.dbaas.client.arangodb.configuration.DbaasArangoDBClientProperties;
import com.netcracker.cloud.dbaas.client.arangodb.configuration.DbaasArangoDBConfigurationProperties;
import com.netcracker.cloud.dbaas.client.arangodb.entity.connection.ArangoConnection;
import com.netcracker.cloud.dbaas.client.arangodb.entity.database.ArangoDatabase;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static com.netcracker.cloud.dbaas.client.arangodb.test.ArangoTestCommon.*;

public class ArangoPostProcessorTest {

    public final static String DB_USER = "dbaas";
    public final static String DB_PASSWORD = "dbaas";

    @Test
    public void testLoadPropertiesFromConfigAcquireHostListTrue() throws NoSuchFieldException, IllegalAccessException {
        Map<String, String> arangoConfig = new HashMap<>();
        arangoConfig.put("acquireHostList", "true");
        arangoConfig.put("acquireHostListInterval", "12345");

        DbaasArangoDBConfigurationProperties config = new DbaasArangoDBConfigurationProperties();
        config.setArangodb(arangoConfig);
        DbaasArangoDBClientProperties dbaasArangoDBClientProperties = new DbaasArangoDBClientProperties(config, null);
        ArangoPostProcessor processor = new ArangoPostProcessor(dbaasArangoDBClientProperties);
        ArangoDB.Builder builderWithLoadedProperties = processor.loadPropertiesFromConfig(new ArangoDB.Builder());

        ArangoConfig builderConfig = (ArangoConfig) getFieldValue(builderWithLoadedProperties, "config");

        assertEquals(true, builderConfig.getAcquireHostList());
        Assertions.assertEquals(12345, builderConfig.getAcquireHostListInterval());
    }

    @Test
    public void testLoadPropertiesFromConfigAcquireHostListFalse() throws NoSuchFieldException, IllegalAccessException {
        Map<String, String> arangoConfig = new HashMap<>();
        arangoConfig.put("acquireHostList", "false");
        arangoConfig.put("acquireHostListInterval", "54321");

        DbaasArangoDBConfigurationProperties config = new DbaasArangoDBConfigurationProperties();
        config.setArangodb(arangoConfig);
        DbaasArangoDBClientProperties dbaasArangoDBClientProperties = new DbaasArangoDBClientProperties(config, null);
        ArangoPostProcessor processor = new ArangoPostProcessor(dbaasArangoDBClientProperties);

        ArangoDB.Builder builderWithLoadedProperties = processor.loadPropertiesFromConfig(new ArangoDB.Builder());

        ArangoConfig builderConfig = (ArangoConfig) getFieldValue(builderWithLoadedProperties, "config");

        assertEquals(false, builderConfig.getAcquireHostList());
        Assertions.assertEquals(54321, builderConfig.getAcquireHostListInterval());
    }

    @Test
    public void checkCanGetCredentialsFromDbaas() {
        String username = "user";
        String password = "passwd";
        DbaasArangoDBConfigurationProperties config = new DbaasArangoDBConfigurationProperties();
        DbaasArangoDBClientProperties dbaasArangoDBClientProperties = new DbaasArangoDBClientProperties(config, null);
        ArangoPostProcessor processor = new ArangoPostProcessor(dbaasArangoDBClientProperties);
        ArangoDatabase database = createArangoDatabase(DB_NAME, DB_HOST, DB_PORT);
        database.setConnectionProperties(setCredentials(username, password, database));
        processor.process(database);
        com.arangodb.ArangoDatabase arangoDatabase = database.getConnectionProperties().getArangoDatabase();
        Assertions.assertNotNull(arangoDatabase);
    }

    @Test
    void mustContainArangoSerdeConfiguration() {
        ArangoDatabase arangoDatabase = createArangoDatabase(DB_NAME, DB_HOST, DB_PORT);
        ArangoConnection connection = setCredentials(DB_USER, DB_PASSWORD, arangoDatabase);
        arangoDatabase.setConnectionProperties(connection);

        ArangoSerde arangoSerde = mock(ArangoSerde.class);
        DbaasArangoDBConfigurationProperties config = Mockito.mock(DbaasArangoDBConfigurationProperties.class);
        DbaasArangoDBClientProperties dbaasArangoDBClientProperties = new DbaasArangoDBClientProperties(config, arangoSerde);
        ArangoPostProcessor processor = spy(new ArangoPostProcessor(dbaasArangoDBClientProperties));
        ArangoDB.Builder arangoDBbuilder = spy(ArangoDB.Builder.class);
        doReturn(arangoDBbuilder).when(processor).loadPropertiesFromConfig(any());
        processor.process(arangoDatabase);

        Assertions.assertEquals(arangoSerde, processor.dbaasArangoDBClientProperties.getArangoSerde());
        verify(arangoDBbuilder, times(1)).serde(arangoSerde);
    }

    @Test
    public void getSupportedDatabaseType() {
        DbaasArangoDBConfigurationProperties config = Mockito.mock(DbaasArangoDBConfigurationProperties.class);
        DbaasArangoDBClientProperties dbaasArangoDBClientProperties = new DbaasArangoDBClientProperties(config, null);

        ArangoPostProcessor processor = new ArangoPostProcessor(dbaasArangoDBClientProperties);
        assertEquals(ArangoDatabase.class, processor.getSupportedDatabaseType());
    }

    private ArangoConnection setCredentials(String username, String password, ArangoDatabase database) {
        ArangoConnection connection = database.getConnectionProperties();
        connection.setUsername(username);
        connection.setPassword(password);
        database.setConnectionProperties(connection);
        return connection;
    }

    private Object getFieldValue(Object obj, String field) throws NoSuchFieldException, IllegalAccessException {
        Class clazz = obj.getClass();
        Field declaredField = null;
        while (clazz != null && declaredField == null) {
            try {
                declaredField = clazz.getDeclaredField(field);
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        if (declaredField == null) {
            throw new NoSuchFieldException();
        }
        declaredField.setAccessible(true);
        return declaredField.get(obj);
    }
}

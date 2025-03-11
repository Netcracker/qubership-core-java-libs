package org.qubership.cloud.quarkus.dbaas.mongoclient;

import org.qubership.cloud.quarkus.dbaas.mongoclient.config.properties.DbaasMongoDbCreationConfig;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class MongoConfigTest {

    public static final String DB_NAME_PREFIX = "quarkus.dbaas.mongo.api.db-prefix";
    public static final String USER_ROLE = "quarkus.dbaas.mongo.api.runtime-user-role";


    @QuarkusTest
    @TestProfile(ExactDbPrefixAndRoleTestProfile.class)
    public static class WithDbPrefixAndRoleTest {
        @Inject
        DbaasMongoDbCreationConfig config;

        @Test
        public void testExtensionsContainsPrefixAndRole() {
            Optional<String> dbPrefix = config.dbaasApiPropertiesConfig.dbPrefix;
            Optional<String> userRole = config.dbaasApiPropertiesConfig.runtimeUserRole;
            Assertions.assertNotNull(dbPrefix);
            Assertions.assertEquals("test-prefix", dbPrefix.get());
            Assertions.assertNotNull(userRole);
            Assertions.assertEquals("admin", userRole.get());
        }
    }

    private static Map<String, String> getBaseProperties() {
        Map<String, String> properties = new HashMap<>();

        properties.put("quarkus.datasource.devservices", "false");
        properties.put("cloud.microservice.name", "dbaas-client-mongo-params-test");
        properties.put("cloud.microservice.namespace", "test-namespace");
        properties.put("quarkus.http.test-port", "0");
        properties.put("quarkus.http.test-ssl-port", "0");

        return properties;
    }

    @NoArgsConstructor
    protected static final class ExactDbPrefixAndRoleTestProfile implements QuarkusTestProfile {
        protected static final String DB_PREFIX_PROPERTY_VAL = "test-prefix";
        protected static final String USER_ROLE_VAL = "admin";

        @Override
        public Map<String, String> getConfigOverrides() {
            Map<String, String> properties = getBaseProperties();
            properties.put(DB_NAME_PREFIX, DB_PREFIX_PROPERTY_VAL);
            properties.put(USER_ROLE, USER_ROLE_VAL);
            return properties;
        }
    }
}

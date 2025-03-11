package org.qubership.cloud.quarkus.dbaas.opensearch.client.it;

import org.qubership.cloud.quarkus.dbaas.opensearch.client.config.DbaaSOpensearchConfigurationProperty;
import org.qubership.cloud.quarkus.dbaas.opensearch.client.config.DbaaSOpensearchCreationConfig;
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

public class ConfigurationTest {

    public static final String USER_ROLE = "quarkus.dbaas.opensearch.api.runtime-user-role";
    public static final String MAX_CONN_TOTAL = "quarkus.dbaas.opensearch.max-conn-total";
    public static final String MAX_CONN_PER_ROUTE = "quarkus.dbaas.opensearch.max-conn-per-route";


    @QuarkusTest
    @TestProfile(RoleTestProfile.class)
    public static class UserRoleTest {
        @Inject
        DbaaSOpensearchCreationConfig config;

        @Test
        public void testExtensionsContainsPrefixAndRole() {
            Optional<String> userRole = config.runtimeUserRole;
            Assertions.assertNotNull(userRole);
            Assertions.assertEquals("rw", userRole.get());
        }
    }

    @QuarkusTest
    @TestProfile(ConfigurationTestProfile.class)
    public static class ConnectionTest {
        @Inject
        DbaaSOpensearchConfigurationProperty configurationProperty;

        @Test
        public void testExtensionsContainsPrefixAndRole() {
            Optional<Integer> maxConnTotal = configurationProperty.maxConnTotal;
            Optional<Integer> maxConnPerRoute = configurationProperty.maxConnPerRoute;
            Assertions.assertTrue(maxConnTotal.isPresent());
            Assertions.assertTrue(maxConnPerRoute.isPresent());
            Assertions.assertEquals(Integer.valueOf(50), maxConnTotal.get());
            Assertions.assertEquals(Integer.valueOf(50), maxConnPerRoute.get());
        }
    }

    @NoArgsConstructor
    protected static final class RoleTestProfile implements QuarkusTestProfile {
        protected static final String USER_ROLE_VAL = "rw";

        @Override
        public Map<String, String> getConfigOverrides() {
            Map<String, String> properties = getBaseProperties();
            properties.put(USER_ROLE, USER_ROLE_VAL);
            return properties;
        }
    }

    @NoArgsConstructor
    protected static final class ConfigurationTestProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            Map<String, String> properties = getBaseProperties();
            properties.put(MAX_CONN_TOTAL, "50");
            properties.put(MAX_CONN_PER_ROUTE, "50");
            return properties;
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

}

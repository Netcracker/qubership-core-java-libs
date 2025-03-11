package org.qubership.cloud.dbaas.common.config;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
@TestProfile(DbaasClientConfigTest.PropertiesProviderTestProfile.class)
class DbaasClientConfigTest {

    public static final String DBAAS_AGGREGATOR_ADDRESS = "https://dbaas-aggregator.qubership.org";
    public static final String DBAAS_USERNAME = "dbaas_username";
    public static final String DBAAS_PASSWORD = "dbaas_password";

    @Inject
    DbaasClientConfig dbaasClientConfig;

    @Test
    void testDbaasClientConfigBuild() {
        assertTrue(dbaasClientConfig.dbaasAgentUrl.isEmpty());
        assertEquals(DBAAS_AGGREGATOR_ADDRESS, dbaasClientConfig.dbaasUrl.get());
        assertEquals(DBAAS_USERNAME, dbaasClientConfig.dbaasUsername.get());
        assertEquals(DBAAS_PASSWORD, dbaasClientConfig.dbaasPassword.get());
    }

    @NoArgsConstructor
    protected static final class PropertiesProviderTestProfile implements QuarkusTestProfile {

        @Override
        public Map<String, String> getConfigOverrides() {
            Map<String, String> properties = new HashMap<>();
            properties.put("cloud.microservice.name", "dbaas-client-params-test");
            properties.put("cloud.microservice.namespace", "test-namespace");
            properties.put("quarkus.dbaas.api.aggregator.address", DBAAS_AGGREGATOR_ADDRESS);
            properties.put("quarkus.dbaas.api.aggregator.username", DBAAS_USERNAME);
            properties.put("quarkus.dbaas.api.aggregator.password", DBAAS_PASSWORD);
            return properties;
        }
    }
}

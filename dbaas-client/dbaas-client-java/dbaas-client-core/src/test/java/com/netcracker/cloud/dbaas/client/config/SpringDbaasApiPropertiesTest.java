package com.netcracker.cloud.dbaas.client.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.util.ReflectionTestUtils;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(SystemStubsExtension.class)
class SpringDbaasApiPropertiesTest {

    @SystemStub
    private EnvironmentVariables environmentVariables;

    @BeforeEach
    void setUp() {
        environmentVariables.set("KUBERNETES_M2M_ENABLED", "true");
    }

    @AfterEach
    void tearDown() {
        environmentVariables.remove("KUBERNETES_M2M_ENABLED");
    }

    @Test
    void testGetAddress() {
        SpringDbaasApiProperties properties = new SpringDbaasApiProperties();

        // Test non-k8s path
        environmentVariables.set("KUBERNETES_M2M_ENABLED", "false");
        ReflectionTestUtils.setField(properties, "dbaasAgentAddress", Optional.of("http://custom"));
        assertEquals("http://custom", properties.getAddress());

        ReflectionTestUtils.setField(properties, "dbaasAgentAddress", Optional.empty());
        assertEquals("http://dbaas-agent:8080", properties.getAddress());

        // Test k8s path
        environmentVariables.set("KUBERNETES_M2M_ENABLED", "true");
        ReflectionTestUtils.setField(properties, "dbaasAddress", Optional.of("http://k8s-url"));
        assertEquals("http://k8s-url", properties.getAddress());

        ReflectionTestUtils.setField(properties, "dbaasAddress", Optional.empty());
        assertEquals("http://dbaas-agent:8080", properties.getAddress());
    }
}

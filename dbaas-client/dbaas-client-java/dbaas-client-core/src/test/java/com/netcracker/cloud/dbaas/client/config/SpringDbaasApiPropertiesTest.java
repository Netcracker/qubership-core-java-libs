package com.netcracker.cloud.dbaas.client.config;

import com.netcracker.cloud.security.core.utils.k8s.M2MAuthMode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.util.ReflectionTestUtils;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(SystemStubsExtension.class)
class SpringDbaasApiPropertiesTest {

    @SystemStub
    private EnvironmentVariables environmentVariables;

    private SpringDbaasApiProperties properties(Optional<String> agentAddress, Optional<String> dbaasAddress) {
        SpringDbaasApiProperties properties = new SpringDbaasApiProperties();
        ReflectionTestUtils.setField(properties, "dbaasAgentAddress", agentAddress);
        ReflectionTestUtils.setField(properties, "dbaasAddress", dbaasAddress);
        return properties;
    }

    @Test
    void legacyModeUsesTheConfiguredAgent() {
        environmentVariables.set(M2MAuthMode.ENV, "legacy");

        assertEquals("http://custom", properties(Optional.of("http://custom"), Optional.of("http://k8s-url")).getAddress());
    }

    @Test
    void legacyModeUsesTheDefaultAgent() {
        environmentVariables.remove(M2MAuthMode.ENV);

        assertEquals("http://dbaas-agent:8080", properties(Optional.empty(), Optional.of("http://k8s-url")).getAddress());
    }

    @Test
    void hybridModeUsesTheDbaasAddress() {
        environmentVariables.set(M2MAuthMode.ENV, "hybrid");

        assertEquals("http://k8s-url", properties(Optional.empty(), Optional.of("http://k8s-url")).getAddress());
    }

    @Test
    void hybridModeFallsBackToTheAgentWithoutDbaasAddress() {
        environmentVariables.set(M2MAuthMode.ENV, "hybrid");

        assertEquals("http://dbaas-agent:8080", properties(Optional.empty(), Optional.empty()).getAddress());
    }

    @Test
    void k8sModeUsesTheDbaasAddress() {
        environmentVariables.set(M2MAuthMode.ENV, "k8s");

        assertEquals("http://k8s-url", properties(Optional.of("http://custom"), Optional.of("http://k8s-url")).getAddress());
    }

    @Test
    void k8sModeRejectsMissingDbaasAddress() {
        environmentVariables.set(M2MAuthMode.ENV, "k8s");
        SpringDbaasApiProperties properties = properties(Optional.of("http://custom"), Optional.empty());

        IllegalStateException e = assertThrows(IllegalStateException.class, properties::getAddress);
        assertTrue(e.getMessage().startsWith("api.dbaas.address is not set"), e.getMessage());
    }
}

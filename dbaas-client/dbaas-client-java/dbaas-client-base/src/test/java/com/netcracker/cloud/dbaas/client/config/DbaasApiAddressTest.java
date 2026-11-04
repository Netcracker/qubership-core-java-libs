package com.netcracker.cloud.dbaas.client.config;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.netcracker.cloud.security.core.utils.k8s.M2MAuthMode.HYBRID;
import static com.netcracker.cloud.security.core.utils.k8s.M2MAuthMode.K8S;
import static com.netcracker.cloud.security.core.utils.k8s.M2MAuthMode.LEGACY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class DbaasApiAddressTest {

    private static final String AGENT = "http://agent:8080";
    private static final String DBAAS = "http://dbaas:8080";

    @BeforeAll
    static void requireNoConfiguredAddresses() {
        assumeTrue(System.getenv(DbaasApiAddress.AGENT_ADDRESS_ENV) == null
                && System.getenv(DbaasApiAddress.DBAAS_ADDRESS_ENV) == null);
    }

    @BeforeEach
    void clearConfiguration() {
        System.clearProperty(DbaasApiAddress.AGENT_ADDRESS_PROPERTY);
        System.clearProperty(DbaasApiAddress.DBAAS_ADDRESS_PROPERTY);
    }

    @Test
    void legacyCallsAgentEvenWhenDbaasAddressIsKnown() {
        assertEquals(AGENT, DbaasApiAddress.resolve(LEGACY, AGENT, DBAAS).url());
    }

    @Test
    void hybridCallsDbaasAndFallsBackToAgent() {
        assertEquals(DBAAS, DbaasApiAddress.resolve(HYBRID, AGENT, DBAAS).url());
        assertEquals(AGENT, DbaasApiAddress.resolve(HYBRID, AGENT, null).url());
        assertEquals(AGENT, DbaasApiAddress.resolve(HYBRID, AGENT, " ").url());
    }

    @Test
    void k8sCallsDbaasAndRequiresItsAddress() {
        assertEquals(DBAAS, DbaasApiAddress.resolve(K8S, AGENT, DBAAS).url());

        var exception = assertThrows(IllegalStateException.class, () -> DbaasApiAddress.resolve(K8S, AGENT, null));
        assertTrue(exception.getMessage().contains(DbaasApiAddress.DBAAS_ADDRESS_PROPERTY), exception.getMessage());
    }

    @Test
    void agentIsUsedAsUrlUntilDbaasAddressAppears() {
        DbaasApiAddress resolved = DbaasApiAddress.resolve(HYBRID, AGENT, null);

        assertEquals(AGENT, resolved.agentUrl());
        assertEquals(AGENT, resolved.url());
    }

    @Test
    void missingAgentAddressFallsBackToDefault() {
        assertEquals(DbaasApiAddress.DEFAULT_AGENT_ADDRESS, DbaasApiAddress.resolve(LEGACY, null, null).agentUrl());
    }

    @Test
    void addressesComeFromConfiguration() {
        System.setProperty(DbaasApiAddress.AGENT_ADDRESS_PROPERTY, AGENT);
        System.setProperty(DbaasApiAddress.DBAAS_ADDRESS_PROPERTY, DBAAS);

        DbaasApiAddress resolved = DbaasApiAddress.resolve(HYBRID);

        assertEquals(AGENT, resolved.agentUrl());
        assertEquals(DBAAS, resolved.url());
    }

    @Test
    void blankConfigurationIsNotAnAddress() {
        System.setProperty(DbaasApiAddress.AGENT_ADDRESS_PROPERTY, " ");
        System.setProperty(DbaasApiAddress.DBAAS_ADDRESS_PROPERTY, " ");

        assertEquals(DbaasApiAddress.DEFAULT_AGENT_ADDRESS, DbaasApiAddress.resolve(LEGACY).agentUrl());
    }

    @Test
    void explicitAddressesWinOverConfiguration() {
        System.setProperty(DbaasApiAddress.AGENT_ADDRESS_PROPERTY, "http://configured-agent:8080");
        System.setProperty(DbaasApiAddress.DBAAS_ADDRESS_PROPERTY, "http://configured-dbaas:8080");

        DbaasApiAddress resolved = DbaasApiAddress.resolve(HYBRID, AGENT, DBAAS);

        assertEquals(AGENT, resolved.agentUrl());
        assertEquals(DBAAS, resolved.url());
    }
}

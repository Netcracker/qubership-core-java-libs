package com.netcracker.cloud.dbaas.common.config;

import okhttp3.OkHttpClient;
import com.netcracker.cloud.security.core.utils.k8s.M2MAuthMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.netcracker.cloud.dbaas.client.DbaasClient;
import org.junit.jupiter.api.extension.ExtendWith;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

import java.lang.reflect.Field;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(SystemStubsExtension.class)
class M2MDbaaSClientTest {
    private M2MDbaaSClient m2MDbaaSClient;
    private DbaasClientConfig dbaasClientConfig;
    private OkHttpClient dbaasOkHttpClient;
    private static final String DB_AGENT_URL  = "http://dbaas-agent:8080";
    private static final String DB_AGGREGATOR_URL  = "http://dbaas-aggregator:8080";

    @SystemStub
    private EnvironmentVariables environmentVariables;

    @BeforeEach
    void setUp() {
        environmentVariables.set(M2MAuthMode.ENV, "hybrid");

        dbaasClientConfig = mock(DbaasClientConfig.class);
        when(dbaasClientConfig.dbaasAgentUrl()).thenReturn(DB_AGENT_URL);
        dbaasOkHttpClient = new DbaasClientProducer().dbaasOkHttpClient(dbaasClientConfig);

        m2MDbaaSClient = new M2MDbaaSClient(Optional.of(DB_AGGREGATOR_URL), dbaasOkHttpClient, dbaasClientConfig);
    }

    @AfterEach
    void tearDown() {
        environmentVariables.remove(M2MAuthMode.ENV);
    }

    @Test
    void testBuild() throws NoSuchFieldException, IllegalAccessException {
        DbaasClient client = m2MDbaaSClient.build();
        Field clientField = client.getClass().getDeclaredField("client");
        clientField.setAccessible(true);
        OkHttpClient clientValue = (OkHttpClient) clientField.get(client);
        assertNotNull(client);
        assertEquals(3, clientValue.interceptors().size());
    }

    @Test
    void testAggregatorAddressIsUsedInHybridMode() throws Exception {
        assertEquals(DB_AGGREGATOR_URL, address(m2MDbaaSClient.build()));
    }

    @Test
    void testAgentAddressIsUsedInLegacyMode() throws Exception {
        environmentVariables.set(M2MAuthMode.ENV, "legacy");

        assertEquals(DB_AGENT_URL, address(m2MDbaaSClient.build()));
    }

    @Test
    void testAgentAddressIsUsedWhenAggregatorAddressIsMissing() throws Exception {
        M2MDbaaSClient withoutAggregatorAddress =
                new M2MDbaaSClient(Optional.empty(), dbaasOkHttpClient, dbaasClientConfig);

        assertEquals(DB_AGENT_URL, address(withoutAggregatorAddress.build()));
    }

    @Test
    void testAggregatorAddressIsUsedInK8sMode() throws Exception {
        environmentVariables.set(M2MAuthMode.ENV, "k8s");

        assertEquals(DB_AGGREGATOR_URL, address(m2MDbaaSClient.build()));
    }

    @Test
    void testMissingAggregatorAddressIsRejectedInK8sMode() {
        environmentVariables.set(M2MAuthMode.ENV, "k8s");
        M2MDbaaSClient withoutAggregatorAddress =
                new M2MDbaaSClient(Optional.empty(), dbaasOkHttpClient, dbaasClientConfig);

        IllegalStateException e = assertThrows(IllegalStateException.class, withoutAggregatorAddress::build);
        assertTrue(e.getMessage().startsWith("api.dbaas.address is not set"), e.getMessage());
    }

    private String address(DbaasClient client) throws Exception {
        Field addressField = client.getClass().getDeclaredField("address");
        addressField.setAccessible(true);
        return (String) addressField.get(client);
    }
}

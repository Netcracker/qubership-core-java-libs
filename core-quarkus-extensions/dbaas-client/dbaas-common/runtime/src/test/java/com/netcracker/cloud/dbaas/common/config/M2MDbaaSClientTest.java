package com.netcracker.cloud.dbaas.common.config;

import okhttp3.OkHttpClient;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(SystemStubsExtension.class)
class M2MDbaaSClientTest {
    private M2MDbaaSClient m2MDbaaSClient;
    private static final String DB_AGENT_URL  = "http://dbaas-agent:8080";
    private static final String DB_AGGREGATOR_URL  = "http://dbaas-aggregator:8080";

    @SystemStub
    private EnvironmentVariables environmentVariables;

    @BeforeEach
    void setUp() {
        DbaasClientConfig dbaasConfig = mock(DbaasClientConfig.class);
        when(dbaasConfig.dbaasAgentUrl()).thenReturn(Optional.of(DB_AGENT_URL));
        when(dbaasConfig.dbaasUrl()).thenReturn(Optional.of(DB_AGGREGATOR_URL));
        environmentVariables.set("SECURITY_M2M_KUBERNETES_ENABLED", "true");

        m2MDbaaSClient = new M2MDbaaSClient(dbaasConfig);
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
}

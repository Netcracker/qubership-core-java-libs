package com.netcracker.cloud.dbaas.common.config;

import okhttp3.OkHttpClient;
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

@ExtendWith(SystemStubsExtension.class)
class M2MDbaaSClientTest {
    private M2MDbaaSClient m2MDbaaSClient;
    private static final String DB_AGENT_URL  = "http://dbaas-agent:8080";
    private static final String DB_AGGREGATOR_URL  = "http://dbaas-aggregator:8080";

    @SystemStub
    private EnvironmentVariables environmentVariables;

    @BeforeEach
    void setUp() {
        environmentVariables.set("KUBERNETES_M2M_ENABLED", "true");

        m2MDbaaSClient = new M2MDbaaSClient();
        m2MDbaaSClient.apiDbaasAddress = Optional.of(DB_AGGREGATOR_URL);
        m2MDbaaSClient.dbaasAgentUrl = DB_AGENT_URL;
        m2MDbaaSClient.dbaasOkHttpClient = new DbaasClientProducer().dbaasOkHttpClient(DB_AGENT_URL);
    }

    @AfterEach
    void tearDown() {
        environmentVariables.remove("KUBERNETES_M2M_ENABLED");
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

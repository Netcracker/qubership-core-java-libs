package org.qubership.cloud.dbaas.common.config;

import okhttp3.OkHttpClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.qubership.cloud.dbaas.client.DbaasClient;

import java.lang.reflect.Field;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class M2MDbaaSClientTest {
    private M2MDbaaSClient m2MDbaaSClient;
    private static final String DB_AGENT_URL  = "http://dbaas-agent:8080";
    @BeforeEach
    void setUp() {
        DbaasClientConfig config = new DbaasClientConfig();
        config.dbaasAgentUrl = Optional.of(DB_AGENT_URL);
        m2MDbaaSClient = new M2MDbaaSClient(config);
    }
    @Test
    void testBuild() throws NoSuchFieldException, IllegalAccessException {
        DbaasClient client = m2MDbaaSClient.build();
        Field clientField = client.getClass().getDeclaredField("client");
        clientField.setAccessible(true);
        OkHttpClient clientValue = (OkHttpClient) clientField.get(client);
        assertNotNull(client);
        assertEquals(2, clientValue.interceptors().size());
    }
}



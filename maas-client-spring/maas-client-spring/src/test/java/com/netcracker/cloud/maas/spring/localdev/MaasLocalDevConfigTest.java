package com.netcracker.cloud.maas.spring.localdev;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.qubership.cloud.security.core.auth.M2MManager;
import org.qubership.cloud.security.core.auth.Token;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;

@SpringBootTest(classes = {MaasLocalDevConfigTest.TestConfig.class, MaasLocalDevConfig.class},
        properties = {"maas.local-dev.enabled=true"})
public class MaasLocalDevConfigTest {

    static class TestConfig {
        @Bean
        M2MManager testM2MManager() {
            return Mockito.mock(M2MManager.class);
        }
    }

    @Autowired(required = false)
    M2MManager m2MManager;

    @Test
    void testLocalM2mManager() {
        Assertions.assertNotNull(m2MManager);
        Assertions.assertEquals(Token.DUMMY_TOKEN, m2MManager.getToken());
    }

}

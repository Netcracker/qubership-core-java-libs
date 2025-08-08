package com.netcracker.cloud.dbaas.client.arangodb.configuration;

import org.qubership.cloud.dbaas.client.arangodb.test.configuration.MockedArangoDBConfiguration;
import org.qubership.cloud.dbaas.client.entity.DbaasApiProperties;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;


@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {MockedArangoDBConfiguration.class})
@TestPropertySource(properties = {
        "dbaas.api.arangodb.retry-attempts=5",
        "dbaas.api.arangodb.retry-delay=200"
})
class DbaasArangoDBConfigurationPropertiesTest {
    @Autowired
    private DbaasArangoDBConfigurationProperties arangoConfig;

    @Test
    void testArangodbPropertiesNotNullWhenNotSet() {
        Assertions.assertNotNull(arangoConfig.getArangodb());
    }

    @Test
    void test() {
        DbaasApiProperties arangodbDbaasApiProperties = arangoConfig.dbaasApiProperties();
        Assertions.assertEquals(5, arangodbDbaasApiProperties.getRetryAttempts());
        Assertions.assertEquals(200L, arangodbDbaasApiProperties.getRetryDelay());
    }
}

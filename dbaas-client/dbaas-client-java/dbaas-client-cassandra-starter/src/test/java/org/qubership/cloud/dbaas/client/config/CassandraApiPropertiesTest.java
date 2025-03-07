package org.qubership.cloud.dbaas.client.config;

import org.qubership.cloud.dbaas.client.entity.DbaasApiProperties;
import org.qubership.cloud.restclient.MicroserviceRestClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {CassandraApiPropertiesTest.TestConfig.class},
        properties = {
                "dbaas.api.cassandra.db-prefix=test-prefix",
                "dbaas.api.cassandra.runtime-user-role=admin"
        })
public class CassandraApiPropertiesTest {

    @Autowired
    private DbaasApiProperties cassandraDbaasApiProperties;

    @Test
    public void checkCassandraDbaasApiProperties(){
        Assertions.assertEquals("test-prefix", cassandraDbaasApiProperties.getDbPrefix());
        Assertions.assertEquals("admin", cassandraDbaasApiProperties.getRuntimeUserRole());
    }

    @Configuration
    @EnableDbaasCassandra
    public static class TestConfig {
        @Primary
        @Bean
        @Qualifier("dbaasRestClient")
        public static MicroserviceRestClient microserviceRestClient() {
            return Mockito.mock(MicroserviceRestClient.class);
        }
    }
}

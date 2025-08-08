package com.netcracker.cloud.dbaas.client;

import com.netcracker.cloud.dbaas.client.config.EnableDbaasMongo;
import com.netcracker.cloud.dbaas.client.entity.DbaasApiProperties;
import com.netcracker.cloud.restclient.MicroserviceRestClient;
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
@SpringBootTest(classes = {MongoApiPropertiesTest.TestConfig.class},
        properties = {
                "dbaas.api.mongo.db-prefix=test-prefix",
                "dbaas.api.mongo.runtime-user-role=test-role"
        })
public class MongoApiPropertiesTest {
    @Autowired
    private DbaasApiProperties mongoDbaasApiProperties;

    @Test
    public void checkMongoApiProperties(){
        Assertions.assertEquals("test-prefix", mongoDbaasApiProperties.getDbPrefix());
        Assertions.assertEquals("test-role", mongoDbaasApiProperties.getRuntimeUserRole());
    }

    @Configuration
    @EnableDbaasMongo
    public static class TestConfig {
        @Primary
        @Bean
        @Qualifier("dbaasRestClient")
        public static MicroserviceRestClient microserviceRestClient() {
            return Mockito.mock(MicroserviceRestClient.class);
        }
    }
}

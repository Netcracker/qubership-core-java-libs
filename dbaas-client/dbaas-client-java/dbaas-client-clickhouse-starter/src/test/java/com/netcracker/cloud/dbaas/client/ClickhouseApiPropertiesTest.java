package com.netcracker.cloud.dbaas.client;

import com.netcracker.cloud.dbaas.client.config.annotation.EnableDbaasClickhouse;
import com.netcracker.cloud.dbaas.client.entity.DbaasApiProperties;
import com.netcracker.cloud.dbaas.client.entity.database.DbaasClickhouseDatasourceProperties;
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

import java.io.IOException;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {ClickhouseApiPropertiesTest.TestConfig.class},
        properties = {
                "dbaas.api.clickhouse.db-prefix=test-prefix",
                "dbaas.api.clickhouse.runtime-user-role=test-role",
                "dbaas.clickhouse.datasource_properties.ssl=true"
        })
class ClickhouseApiPropertiesTest {

    @Autowired
    private DbaasApiProperties clichouseDbaasApiProperties;

    @Autowired
    private DbaasClickhouseDatasourceProperties datasourceProperties;

    @Test
    void checkClickhouseApiProperties(){
        Assertions.assertEquals("test-prefix", clichouseDbaasApiProperties.getDbPrefix());
        Assertions.assertEquals("test-role", clichouseDbaasApiProperties.getRuntimeUserRole());
    }

    @Test
    void checkClickhouseDatasourceProperties() throws IOException {
        Assertions.assertEquals("true",datasourceProperties.getDatasourceProperties().get("ssl"));
    }

    @Configuration
    @EnableDbaasClickhouse
    public static class TestConfig {
        @Primary
        @Bean
        @Qualifier("dbaasRestClient")
        public static MicroserviceRestClient microserviceRestClient() {
            return Mockito.mock(MicroserviceRestClient.class);
        }
    }
}

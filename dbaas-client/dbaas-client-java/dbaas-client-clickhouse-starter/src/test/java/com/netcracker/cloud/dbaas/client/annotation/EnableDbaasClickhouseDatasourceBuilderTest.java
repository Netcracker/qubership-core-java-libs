package com.netcracker.cloud.dbaas.client.annotation;

import org.qubership.cloud.dbaas.client.config.annotation.EnableDbaasClickhouseDatasourceBuilder;
import org.qubership.cloud.dbaas.client.management.DbaasClickhouseDatasourceBuilder;
import org.qubership.cloud.restclient.MicroserviceRestClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;


@SpringBootTest(classes = EnableDbaasClickhouseDatasourceBuilderTest.ConfigurationTest.class)
class EnableDbaasClickhouseDatasourceBuilderTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void checkExistenceBeansTest() {
        Assertions.assertNotNull(applicationContext.getBean(DbaasClickhouseDatasourceBuilder.class));
        assertThatExceptionOfType(NoSuchBeanDefinitionException.class).isThrownBy(() -> applicationContext.getBean(DataSource.class));
    }

    @Configuration
    @EnableDbaasClickhouseDatasourceBuilder
    static class ConfigurationTest {
        @Primary
        @Bean
        @Qualifier("dbaasRestClient")
        public static MicroserviceRestClient microserviceRestClient() {
            return Mockito.mock(MicroserviceRestClient.class);
        }
    }
}

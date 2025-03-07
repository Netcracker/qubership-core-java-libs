package org.qubership.cloud.dbaas.client.test.configuration;

import org.qubership.cloud.dbaas.client.DbaasClient;
import org.qubership.cloud.dbaas.client.DbaasClientImpl;
import org.qubership.cloud.dbaas.client.config.EnableDbaasDefault;
import org.qubership.cloud.dbaas.client.entity.database.MongoDatabase;
import org.qubership.cloud.dbaas.client.management.PostConnectProcessor;
import org.qubership.cloud.restclient.MicroserviceRestClient;
import lombok.extern.slf4j.Slf4j;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.retry.support.RetryTemplate;

@Slf4j
@Configuration
@EnableDbaasDefault
public class CreateDatabaseConfig {

    @Bean
    @Primary
    @Qualifier("dbaasRestClient")
    public MicroserviceRestClient testDbaasRestClient(){
        return Mockito.mock(MicroserviceRestClient.class);
    }

    @Bean
    @Primary
    public DbaasClient testDbaasClient (@Qualifier("dbaasClientDefaultRetryTemplate") RetryTemplate retryTemplate,
                                        @Qualifier("dbaasRestClient") MicroserviceRestClient testDbaasRestClient) {
        return new DbaasClientImpl(testDbaasRestClient, retryTemplate, "http://ms-name.namespace:8080");
    }

    @Bean("testMongoPostProcessor")
    @Primary
    public PostConnectProcessor<MongoDatabase> testMongoDatabasePostProcessor() {
        PostConnectProcessor mock = Mockito.mock(PostConnectProcessor.class);
        return mock;
    }
}

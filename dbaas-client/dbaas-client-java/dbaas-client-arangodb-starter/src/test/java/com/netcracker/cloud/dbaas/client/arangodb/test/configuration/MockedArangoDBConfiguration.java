package com.netcracker.cloud.dbaas.client.arangodb.test.configuration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.isNull;
import static org.mockito.Mockito.when;

import org.qubership.cloud.dbaas.client.DbaasClient;
import org.qubership.cloud.dbaas.client.DbaasClientImpl;
import org.qubership.cloud.dbaas.client.arangodb.configuration.EnableTenantDbaasArangoDB;
import org.qubership.cloud.dbaas.client.arangodb.entity.database.ArangoDatabase;
import org.qubership.cloud.restclient.HttpMethod;
import org.qubership.cloud.restclient.MicroserviceRestClient;
import org.qubership.cloud.restclient.entity.RestClientResponseEntity;

import org.mockito.Mockito;
import org.qubership.cloud.dbaas.client.arangodb.test.ArangoTestCommon;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.retry.support.RetryTemplate;

import java.net.URI;

@Configuration
@EnableTenantDbaasArangoDB
public class MockedArangoDBConfiguration {
    @Bean
    @Primary
    public DbaasClient testDbaasClient(@Autowired(required = false) @Qualifier("dbaasClientDefaultRetryTemplate") RetryTemplate retryTemplate,
                                       @Qualifier("dbaasRestClient") MicroserviceRestClient microserviceRestClient) {
        RestClientResponseEntity<ArangoDatabase> restClientResponseEntity = new RestClientResponseEntity<>(ArangoTestCommon.createArangoDatabase(ArangoTestCommon.DB_NAME, ArangoTestCommon.DB_HOST, ArangoTestCommon.DB_PORT), HttpStatus.CREATED.value(), null);
        when(microserviceRestClient.doRequest(any(URI.class), eq(HttpMethod.PUT), isNull(), any(), eq(ArangoDatabase.class))).thenReturn(restClientResponseEntity);
        return new DbaasClientImpl(microserviceRestClient, retryTemplate, "http://ms-name.namespace:8080");
    }

    @Primary
    @Bean
    @Qualifier("dbaasRestClient")
    public static MicroserviceRestClient microserviceRestClient() {
        return Mockito.mock(MicroserviceRestClient.class);
    }

}

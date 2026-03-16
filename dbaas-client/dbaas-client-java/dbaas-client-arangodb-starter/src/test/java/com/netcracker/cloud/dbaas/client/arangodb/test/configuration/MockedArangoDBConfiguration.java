package com.netcracker.cloud.dbaas.client.arangodb.test.configuration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.isNull;
import static org.mockito.Mockito.when;

import com.netcracker.cloud.dbaas.client.DbaasClient;
import com.netcracker.cloud.dbaas.client.DbaasClientImpl;
import com.netcracker.cloud.dbaas.client.arangodb.configuration.EnableTenantDbaasArangoDB;
import com.netcracker.cloud.dbaas.client.arangodb.entity.database.ArangoDatabase;
import com.netcracker.cloud.restclient.HttpMethod;
import com.netcracker.cloud.restclient.MicroserviceRestClient;
import com.netcracker.cloud.restclient.entity.RestClientResponseEntity;

import org.mockito.Mockito;
import com.netcracker.cloud.dbaas.client.arangodb.test.ArangoTestCommon;
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

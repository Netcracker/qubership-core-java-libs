package org.qubership.cloud.dbaas.client.redis.test.configuration;

import org.qubership.cloud.dbaas.client.DbaasClient;
import org.qubership.cloud.dbaas.client.DbaasClientImpl;
import org.qubership.cloud.dbaas.client.entity.DatabaseCreateRequest;
import org.qubership.cloud.dbaas.client.redis.configuration.annotation.EnableDbaasRedis;
import org.qubership.cloud.dbaas.client.redis.entity.database.RedisDatabase;
import org.qubership.cloud.restclient.HttpMethod;
import org.qubership.cloud.restclient.MicroserviceRestClient;
import org.qubership.cloud.restclient.entity.RestClientResponseEntity;

import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.retry.support.RetryTemplate;

import java.util.Map;

import static org.qubership.cloud.dbaas.client.DbaasConst.SCOPE;
import static org.qubership.cloud.dbaas.client.DbaasConst.SERVICE;
import static org.qubership.cloud.dbaas.client.DbaasConst.TENANT;
import static org.qubership.cloud.dbaas.client.DbaasConst.TENANT_ID;
import static org.qubership.cloud.dbaas.client.redis.test.RedisTestCommon.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.isNull;
import static org.mockito.Mockito.when;

@Configuration
@EnableDbaasRedis
public class MockedRedisDBConfiguration {
    @Bean("testDbaasClient")
    @Primary
    public DbaasClient testDbaasClient(@Autowired(required = false) @Qualifier("dbaasClientDefaultRetryTemplate") RetryTemplate retryTemplate,
                                       @Qualifier("dbaasRestClient") MicroserviceRestClient microserviceRestClient) {
        when(microserviceRestClient.doRequest(any(String.class), eq(HttpMethod.PUT), isNull(), any(DatabaseCreateRequest.class), eq(RedisDatabase.class)))
                .thenAnswer((Answer<RestClientResponseEntity<RedisDatabase>>) invocationOnMock -> {
                    DatabaseCreateRequest databaseCreateRequest = (DatabaseCreateRequest) invocationOnMock.getArguments()[3];
                    Map<String, Object> classifier = databaseCreateRequest.getClassifier();
                    String scope = (String) classifier.get(SCOPE);
                    if (SERVICE.equals(scope)) {
                        String dbName = SERVICE + TEST_DB_NAME;
                        RestClientResponseEntity<RedisDatabase> restClientResponseEntity = new RestClientResponseEntity<>(
                                createRedisDatabase(dbName, TEST_DB_HOST, TEST_DB_PORT, TEST_PASSWORD), HttpStatus.CREATED.value(), null);
                        return restClientResponseEntity;
                    } else if (TENANT.equals(scope)) {
                        String tenantId = (String) classifier.get(TENANT_ID);
                        String dbName = TENANT + tenantId + TEST_DB_NAME;
                        RestClientResponseEntity<RedisDatabase> restClientResponseEntity = new RestClientResponseEntity<>(createRedisDatabase(dbName, TEST_DB_HOST, TEST_DB_PORT, TEST_PASSWORD), HttpStatus.CREATED.value(), null);
                        return restClientResponseEntity;

                    } else {
                        throw new RuntimeException("Invalid classifier scope");
                    }
                });
        return new DbaasClientImpl(microserviceRestClient, retryTemplate, "http://ms-name.namespace:8080");
    }

    @Primary
    @Bean("dbaasRestClient")
    public static MicroserviceRestClient microserviceRestClient() {
        return Mockito.mock(MicroserviceRestClient.class);
    }
}

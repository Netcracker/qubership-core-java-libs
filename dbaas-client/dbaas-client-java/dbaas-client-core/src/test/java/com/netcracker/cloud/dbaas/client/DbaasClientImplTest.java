package com.netcracker.cloud.dbaas.client;

import com.netcracker.cloud.dbaas.client.entity.GetDatabaseByClassifierRequest;
import com.netcracker.cloud.dbaas.client.entity.PhysicalDatabaseDescription;
import com.netcracker.cloud.dbaas.client.entity.PhysicalDatabases;
import com.netcracker.cloud.dbaas.client.entity.database.type.DatabaseType;
import com.netcracker.cloud.dbaas.client.entity.postgres.PostgresDBConnection;
import com.netcracker.cloud.dbaas.client.entity.postgres.PostgresDatabase;
import com.netcracker.cloud.restclient.HttpMethod;
import com.netcracker.cloud.restclient.MicroserviceRestClient;
import com.netcracker.cloud.restclient.entity.RestClientResponseEntity;
import com.netcracker.cloud.restclient.exception.MicroserviceRestClientResponseException;
import lombok.NoArgsConstructor;

import org.apache.hc.core5.http.HttpStatus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryPolicy;
import org.springframework.retry.RetryState;
import org.springframework.retry.context.RetryContextSupport;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.util.UriTemplate;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.netcracker.cloud.dbaas.client.DbaasClientApiConst.ASYNC_CREATE_DATABASE_TEMPLATE_ENDPOINT;
import static com.netcracker.cloud.dbaas.client.DbaasClientApiConst.GET_CONNECTION_TEMPLATE_ENDPOINT;
import static com.netcracker.cloud.dbaas.client.DbaasClientApiConst.GET_PHYSICAL_DATABASES_TEMPLATE_ENDPOINT;
import static com.netcracker.cloud.dbaas.client.DbaasConst.SCOPE;
import static com.netcracker.cloud.dbaas.client.DbaasConst.SERVICE;
import static com.netcracker.cloud.dbaas.client.entity.database.type.PhysicalDbType.POSTGRESQL;

public class DbaasClientImplTest {
    private static final String DBAAS_TEST_HOST = "http://test.dbaas.host:8080";

    private MicroserviceRestClient dbaasRestClient;

    private RetryTemplateStub retryTemplate;


    @BeforeEach
    public void init() {
        this.dbaasRestClient = Mockito.mock(MicroserviceRestClient.class);
        this.retryTemplate = Mockito.spy(RetryTemplateStub.class);
        this.retryTemplate.setRetryPolicy(new SimpleRetryPolicy());
    }

    @Test
    public void testCreateDatabaseRetries_IfResponseIsNot400() {
        MicroserviceRestClientResponseException responseException = new MicroserviceRestClientResponseException(
                "500 error", HttpStatus.SC_INTERNAL_SERVER_ERROR, "500 error".getBytes(), null
        );

        RetryContext retryContextOnV2 = Mockito.mock(RetryContextSupport.class);
        Mockito.doReturn(retryContextOnV2).when(
                retryTemplate).open(Mockito.any(), Mockito.any()
        );

        String namespace = "ns";
        URI uri = new UriTemplate(DBAAS_TEST_HOST + ASYNC_CREATE_DATABASE_TEMPLATE_ENDPOINT).expand(namespace);
        String uriString = uri.toString();

        RestClientResponseEntity responseEntity = new RestClientResponseEntity<>(
                new PostgresDatabase(), org.springframework.http.HttpStatus.CREATED.value(), null
        );

        Mockito.when(dbaasRestClient.doRequest(
                        Mockito.eq(uriString),
                        Mockito.eq(HttpMethod.PUT),
                        Mockito.isNull(),
                        Mockito.any(),
                        Mockito.any()
                ))
                .thenThrow(responseException) // 1
                .thenThrow(responseException) // 2
                .thenReturn(responseEntity);  // 3

        DbaasClientImpl dbaasClient = new DbaasClientImpl(dbaasRestClient, retryTemplate, DBAAS_TEST_HOST);
        dbaasClient = Mockito.spy(dbaasClient);

        DatabaseType<?, ?> dbType = new DatabaseType<>("postgresql", PostgresDatabase.class);
        dbaasClient.getOrCreateDatabase(
                dbType, namespace, TestUtil.buildServiceClassifier("test-namespace", "test-ms")
        );

        Mockito.verify(dbaasRestClient, Mockito.times(3)).doRequest(
                Mockito.eq(uriString),
                Mockito.eq(HttpMethod.PUT),
                Mockito.isNull(),
                Mockito.any(),
                Mockito.any()
        );
    }

    @Test
    public void testCreateDatabaseSendsAsyncRequest() {
        RestClientResponseEntity<Object> createdResponseEntity = Mockito.mock(RestClientResponseEntity.class);
        Mockito.when(createdResponseEntity.getHttpStatus()).thenReturn(HttpStatus.SC_CREATED);
        Mockito.when(createdResponseEntity.getResponseBody()).thenReturn(new PostgresDatabase());

        DbaasClientImpl dbaasClient = new DbaasClientImpl(dbaasRestClient, retryTemplate, DBAAS_TEST_HOST);
        Mockito.when(dbaasRestClient.doRequest(
                        Mockito.contains("async=true"),
                        Mockito.eq(HttpMethod.PUT),
                        Mockito.isNull(),
                        Mockito.any(),
                        Mockito.any()))
                .thenReturn(createdResponseEntity);

        DatabaseType<?, ?> dbType = new DatabaseType<>("postgresql", PostgresDatabase.class);
        String namespace = "ns";
        dbaasClient.getOrCreateDatabase(dbType, namespace, TestUtil.buildServiceClassifier("test-namespace", "test-ms"));

        Mockito.verify(dbaasRestClient, Mockito.times(1)).doRequest(
                Mockito.contains("async=true"),
                Mockito.eq(HttpMethod.PUT),
                Mockito.isNull(),
                Mockito.any(),
                Mockito.any()
        );
    }

    @Test
    public void testCreateDatabaseRetries_IfResponseIsAccepted() {
        RetryContext retryContextOnV2 = Mockito.mock(RetryContextSupport.class);
        Mockito.doReturn(retryContextOnV2).when(
                retryTemplate).open(Mockito.any(), Mockito.any()
        );

        String namespace = "ns";
        URI uri = new UriTemplate(DBAAS_TEST_HOST + ASYNC_CREATE_DATABASE_TEMPLATE_ENDPOINT).expand(namespace);

        RestClientResponseEntity<Object> acceptedResponseEntity = Mockito.mock(RestClientResponseEntity.class);
        Mockito.when(acceptedResponseEntity.getHttpStatus()).thenReturn(HttpStatus.SC_ACCEPTED);
        Mockito.when(acceptedResponseEntity.getResponseBody()).thenReturn(new PostgresDatabase());

        RestClientResponseEntity<Object> createdResponseEntity = Mockito.mock(RestClientResponseEntity.class);
        Mockito.when(createdResponseEntity.getHttpStatus()).thenReturn(HttpStatus.SC_CREATED);
        Mockito.when(createdResponseEntity.getResponseBody()).thenReturn(new PostgresDatabase());

        // two times returned 202-Accepted, then 201-Created
        Mockito.when(dbaasRestClient.doRequest(
                        Mockito.eq(uri.toString()),
                        Mockito.eq(HttpMethod.PUT),
                        Mockito.isNull(),
                        Mockito.any(),
                        Mockito.any()
                ))
                .thenReturn(acceptedResponseEntity)
                .thenReturn(acceptedResponseEntity)
                .thenReturn(createdResponseEntity);

        DbaasClientImpl dbaasClient = new DbaasClientImpl(dbaasRestClient, retryTemplate, DBAAS_TEST_HOST);
        dbaasClient = Mockito.spy(dbaasClient);

        DatabaseType<?, ?> dbType = new DatabaseType<>("postgresql", PostgresDatabase.class);
        dbaasClient.getOrCreateDatabase(dbType, namespace, TestUtil.buildServiceClassifier("test-namespace", "test-ms"));

        // two times response code is 202, and we expect retry, then response code is 201
        Mockito.verify(acceptedResponseEntity, Mockito.times(2)).getHttpStatus();
        Mockito.verify(createdResponseEntity, Mockito.times(1)).getHttpStatus();
    }

    @Test
    public void testGetConnection() {
        String namespace = "ns";
        DatabaseType<?, ?> dbType = new DatabaseType<>("postgresql", PostgresDatabase.class);
        Map<String, Object> classifier = new HashMap<>(2);
        classifier.put("someField", "someValue");
        classifier.put(SCOPE, SERVICE);

        GetDatabaseByClassifierRequest getDatabaseByClassifierRequest = new GetDatabaseByClassifierRequest(classifier, null);

        DbaasClientImpl dbaasClient = new DbaasClientImpl(dbaasRestClient, retryTemplate, DBAAS_TEST_HOST);
        dbaasClient = Mockito.spy(dbaasClient);

        PostgresDatabase postgresqlDb = new PostgresDatabase();
        postgresqlDb.setConnectionProperties(new PostgresDBConnection());
        RestClientResponseEntity connectionEntity = Mockito.mock(RestClientResponseEntity.class);
        Mockito.when(connectionEntity.getResponseBody()).thenReturn(postgresqlDb);

        URI connectionUri = new UriTemplate(DBAAS_TEST_HOST + GET_CONNECTION_TEMPLATE_ENDPOINT).expand(namespace, "postgresql");
        Mockito.when(dbaasRestClient.doRequest(
                        Mockito.eq(connectionUri),
                        Mockito.eq(HttpMethod.POST),
                        Mockito.isNull(),
                        Mockito.eq(getDatabaseByClassifierRequest),
                        Mockito.eq(PostgresDatabase.class)
                ))
                .thenReturn(connectionEntity);

        dbaasClient.getDatabase(dbType, namespace, null, classifier);
        Mockito.verify(dbaasRestClient, Mockito.times(1)).doRequest(
                Mockito.eq(connectionUri),
                Mockito.eq(HttpMethod.POST),
                Mockito.isNull(),
                Mockito.eq(getDatabaseByClassifierRequest),
                Mockito.eq(PostgresDatabase.class)
        );
    }

    @Test
    public void checkGetPhysicalDatabaseRequest() {
        URI uri = new UriTemplate(DBAAS_TEST_HOST + GET_PHYSICAL_DATABASES_TEMPLATE_ENDPOINT).expand(POSTGRESQL);
        PhysicalDatabases physicalDatabases = new PhysicalDatabases();
        physicalDatabases.setIdentified(Collections.singletonMap("pg-id", new PhysicalDatabaseDescription()));
        RestClientResponseEntity<PhysicalDatabases> restClientResponseEntity = new RestClientResponseEntity<>(physicalDatabases, 200);
        Mockito.when(dbaasRestClient.doRequest(uri, HttpMethod.GET, null, null, PhysicalDatabases.class))
                .thenReturn(restClientResponseEntity);
        DbaasClientImpl dbaasClient = new DbaasClientImpl(dbaasRestClient, retryTemplate, DBAAS_TEST_HOST);
        PhysicalDatabases actualPhysDb = dbaasClient.getPhysicalDatabases(POSTGRESQL);
        Assertions.assertEquals(1, actualPhysDb.getIdentified().size());
    }


    @NoArgsConstructor
    private static class RetryTemplateStub extends RetryTemplate {
        @Override
        protected RetryContext open(RetryPolicy retryPolicy, RetryState state) {
            return super.open(retryPolicy, state);
        }

        @Override
        protected void registerThrowable(RetryPolicy retryPolicy, RetryState state, RetryContext context, Throwable e) {
            Mockito.when(context.getLastThrowable()).thenReturn(e);
        }
    }
}

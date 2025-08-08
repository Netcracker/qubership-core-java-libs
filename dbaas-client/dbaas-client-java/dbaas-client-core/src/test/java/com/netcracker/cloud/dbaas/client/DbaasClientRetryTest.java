package com.netcracker.cloud.dbaas.client;

import org.qubership.cloud.dbaas.client.config.TestDbaasCoreConfiguration;
import org.qubership.cloud.dbaas.client.exceptions.DbaasUnavailableException;
import org.qubership.cloud.restclient.HttpMethod;
import org.qubership.cloud.restclient.MicroserviceRestClient;
import org.qubership.cloud.restclient.exception.MicroserviceRestClientException;
import org.qubership.cloud.restclient.exception.MicroserviceRestClientResponseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.net.URI;
import java.util.HashMap;

import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {TestDbaasCoreConfiguration.class})
public class DbaasClientRetryTest {

    @Autowired
    private DbaasClient dbaasClient;

    @Autowired
    private MicroserviceRestClient restClient;

    @BeforeEach
    public void setUp() {
        reset(restClient);  // clear counts
    }

    @Test
    public void testRetryOnCreate() throws Throwable {
        when(restClient.doRequest(any(String.class), any(HttpMethod.class), isNull(), any(), eq(TestDatabase.class)))
                .thenThrow(new MicroserviceRestClientException("dbaas is not available"));
        try {
          dbaasClient.getOrCreateDatabase(TestDBType.INSTANCE, "test-namespace", TestUtil.buildServiceClassifier("test-namespace", "test-ms"));
      } catch (DbaasUnavailableException e) {
          // ignore
      }
        verify(restClient, times(2)).doRequest(any(String.class), any(HttpMethod.class), isNull(), any(), eq(TestDatabase.class));
    }

    @Test
    public void testRetryOnGetConnection() throws Throwable {
        when(restClient.doRequest(any(URI.class), any(HttpMethod.class), isNull(), any(), eq(TestDatabase.class)))
                .thenThrow(new MicroserviceRestClientException("dbaas is not available"));
        try {
            dbaasClient.getDatabase(TestDBType.INSTANCE, "test-namespace", null, new HashMap<>());
        } catch (DbaasUnavailableException e) {
            // ignore
        }
        verify(restClient, times(2)).doRequest(any(URI.class), any(HttpMethod.class), isNull(), any(), eq(TestDatabase.class));
    }


    @Test
    public void testRetryOnGetConnectionWhen404Returned() throws Throwable {
        when(restClient.doRequest(any(URI.class), any(HttpMethod.class), isNull(), any(), eq(TestDatabase.class)))
                .thenThrow(new MicroserviceRestClientResponseException(null, HttpStatus.NOT_FOUND.value(), null, null));
        dbaasClient.getDatabase(TestDBType.INSTANCE, "test-namespace", null, new HashMap<>());
        verify(restClient, times(1)).doRequest(any(URI.class), any(HttpMethod.class), isNull(), any(), eq(TestDatabase.class));
    }

}

package org.qubership.cloud.quarkus.consul.client;

import com.ecwid.consul.transport.TransportException;
import org.qubership.cloud.quarkus.consul.client.model.GetValue;
import org.qubership.cloud.quarkus.consul.client.http.QueryParams;
import org.qubership.cloud.quarkus.consul.client.http.Response;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

class RetrableConsulClientTest {

    ConsulSourceConfig consulDefaultSourceConfig;

    @BeforeEach
    void setUp() {
        this.consulDefaultSourceConfig = new ConsulSourceConfig() {

            @Override
            public boolean enabled() {
                return true;
            }

            @Override
            public AgentConfig agent() {
                return () -> Optional.empty();
            }

            @Override
            public Optional<List<String>> propertiesRoot() {
                return Optional.empty();
            }

            @Override
            public Integer waitTime() {
                return 600;
            }
        };
    }

    @Test
    void continuesAfterFirstError() {
        Response<List<GetValue>> expectedResponse = new Response<>(Collections.emptyList(),
                0L, true, 0L);
        ConsulClient client = Mockito.mock(ConsulClient.class);
        Mockito.when(client.getKVValues(Mockito.anyString(), Mockito.anyString(), Mockito.any(QueryParams.class)))
                .thenThrow(new RuntimeException("consul is not available"))
                .thenReturn(expectedResponse);

        RetryableConsulClient retryableClient = new RetryableConsulClient(client, new TokenStorageStub());
        Response<List<GetValue>> actualResponse = retryableClient.getKVValues("config/namespace/application", new QueryParams(consulDefaultSourceConfig.waitTime(), 0L));
        Assertions.assertEquals(expectedResponse, actualResponse);
    }

    @Test
    void returnsNullOnRetryAttemptsExceeded() {
        ConsulClient client = Mockito.mock(ConsulClient.class);
        Mockito.when(client.getKVValues(Mockito.anyString(), Mockito.anyString(), Mockito.any(QueryParams.class)))
                .thenThrow(new TransportException(new RuntimeException("consul is not available1")));

        RetryableConsulClient retryableClient = new RetryableConsulClient(client, new TokenStorageStub());
        retryableClient.SLEEP_ON_CONSUL_ERROR_PERIOD_MS = 10;
        Response<List<GetValue>> actualResponse = retryableClient.getKVValues( "config/namespace/application", new QueryParams(consulDefaultSourceConfig.waitTime(), 10L));
        Assertions.assertNotNull(actualResponse);
        Assertions.assertNull(actualResponse.getValue());
        Assertions.assertEquals(10L, actualResponse.getConsulIndex());
    }

    @Test
    void oneTryAfterConsideredBad() {
        ConsulClient client = Mockito.mock(ConsulClient.class);
        Mockito.when(client.getKVValues(Mockito.anyString(), Mockito.anyString(), Mockito.any(QueryParams.class)))
                .thenThrow(new RuntimeException("consul is not available1"));

        RetryableConsulClient retryableClient = new RetryableConsulClient(client, new TokenStorageStub());
        retryableClient.SLEEP_ON_CONSUL_ERROR_PERIOD_MS = 10;
        retryableClient.getKVValues( "config/namespace/application", new QueryParams(consulDefaultSourceConfig.waitTime(), 0L)); // 3 times calls client

        Response<List<GetValue>> res = retryableClient.getKVValues("config/namespace/application", new QueryParams(consulDefaultSourceConfig.waitTime(), 0L)); // single client call
        Assertions.assertNotNull(res);
        Assertions.assertNull(res.getValue());
        Mockito.verify(client, Mockito.times(6)).getKVValues(Mockito.anyString(), Mockito.anyString(), Mockito.any(QueryParams.class));
    }
}

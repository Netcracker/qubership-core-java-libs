package com.netcracker.cloud.dbaas.client;

import com.netcracker.cloud.dbaas.client.exceptions.DbaasException;
import com.netcracker.cloud.dbaas.client.exceptions.DbaasUnavailableException;
import com.netcracker.cloud.restclient.HttpMethod;
import com.netcracker.cloud.restclient.MicroserviceRestClient;
import com.netcracker.cloud.restclient.entity.RestClientResponseEntity;
import com.netcracker.cloud.restclient.exception.MicroserviceRestClientException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.util.Assert;
import org.springframework.web.client.RestClientException;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import static com.netcracker.cloud.dbaas.client.DbaasClientApiConst.HEALTH_ENDPOINT;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
public class DbaasClassifierImplTest {

    @Mock
    private MicroserviceRestClient restClient;

    private DbaasClientImpl dbaasClient;

    @BeforeEach
    public void setup() {
        dbaasClient = new DbaasClientImpl(restClient, "http://ms-name.namespace:8080");
    }

    @Test
    public void testClientUnavailable() {
        doThrow(new RestClientException("test rest exception")).when(restClient).doRequest(any(URI.class),
                any(),
                any(),
                any(),
                any(Class.class));
        boolean available = dbaasClient.isAvailable();
        Assert.isTrue(!available, "Dbaas client must return unavailable when exception thrown inside its implementation");
    }

    @Test
    public void testClientThrowsUnavailable() {
        doThrow(new MicroserviceRestClientException("test rest exception")).when(restClient).doRequest(any(String.class),
                any(),
                any(),
                any(),
                any(Class.class));
        assertThrows(DbaasUnavailableException.class,
                () -> dbaasClient.getOrCreateDatabase(TestDBType.INSTANCE, "test", TestUtil.buildServiceClassifier("test", "test-ms")));
    }

    @Test
    public void testClientDoNotThrowsUnavailable() throws URISyntaxException {
        doThrow(new MicroserviceRestClientException("test rest exception")).when(restClient).doRequest(any(String.class),
                any(),
                any(),
                any(),
                any(Class.class));
        Map<String, Object> availableResponseBody = new HashMap<>();
        availableResponseBody.put("status", "UP");
        URI isAvailableUri = URI.create("http://ms-name.namespace:8080" + HEALTH_ENDPOINT);
        when(restClient.doRequest(eq(isAvailableUri),
                eq(HttpMethod.GET),
                isNull(),
                isNull(),
                eq(Map.class))).thenReturn(new RestClientResponseEntity(availableResponseBody, HttpStatus.OK.value(), null));
        assertThrows(DbaasException.class,
                () -> dbaasClient.getOrCreateDatabase(TestDBType.INSTANCE, "test", TestUtil.buildServiceClassifier("test", "test-ms")));
    }
}

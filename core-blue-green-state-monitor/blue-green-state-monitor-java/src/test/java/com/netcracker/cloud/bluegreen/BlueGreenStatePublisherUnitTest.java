package com.netcracker.cloud.bluegreen;

import com.netcracker.cloud.bluegreen.impl.http.HttpClientAdapter;
import com.netcracker.cloud.bluegreen.impl.http.error.DefaultErrorCodeException;
import com.netcracker.cloud.bluegreen.impl.service.ConsulBlueGreenStatePublisher;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpResponse;
import java.time.Duration;


class BlueGreenStatePublisherUnitTest {

    @Test
    void test403FromConsul() throws IOException, InterruptedException {
        HttpClient client = Mockito.mock(HttpClient.class);
        HttpResponse<String> response = Mockito.mock(HttpResponse.class);
        HttpHeaders headers = Mockito.mock(HttpHeaders.class);
        Mockito.when(response.statusCode()).thenReturn(403);
        Mockito.when(response.headers()).thenReturn(headers);
        Mockito.when(response.body()).thenReturn("");

        Mockito.when(client.send(Mockito.any(), Mockito.any(HttpResponse.BodyHandlers.ofString().getClass()))).thenReturn(response);

        try (ConsulBlueGreenStatePublisher publisher = new ConsulBlueGreenStatePublisher(new HttpClientAdapter(client, () -> "fake-token"),
                "http://consul-test:8500", "test-namespace", Duration.ofSeconds(5), Duration.ofSeconds(1))) {
            Assertions.fail();
        } catch (IllegalStateException e) {
            Throwable cause = e.getCause();
            Assertions.assertNotNull(cause);
            Assertions.assertTrue(cause instanceof DefaultErrorCodeException);
            DefaultErrorCodeException errorCodeException = (DefaultErrorCodeException) cause;
            Assertions.assertEquals(403, errorCodeException.getHttpCode());

        }
    }
}

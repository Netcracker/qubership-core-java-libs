package com.netcracker.cloud.dbaas.client;

import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.matchers.Times;
import org.mockserver.verify.VerificationTimes;

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

public class HttpErrorRetrierTest {
    private static final String PATH = "/api/v3/dbaas/postgresql/physical_databases";

    private Request request;
    private Interceptor.Chain mockChain;
    private AtomicInteger failNumber;

    @BeforeEach
    public void setUp() {
        mockChain = Mockito.mock(Interceptor.Chain.class);
        request = new Request.Builder().get().url("http://test-url:8080").build();
        when(mockChain.request()).thenReturn(request);

        failNumber = new AtomicInteger(0);
    }

    @Test
    public void testSuccessOnTheLastTry() throws IOException {
        final int limit = 3;
        final int sleepMultiplier = 1;
        HttpErrorRetrier httpErrorRetrier = new HttpErrorRetrier(Collections.singletonList(503), limit, sleepMultiplier);

        when(mockChain.proceed(any())).thenAnswer((Answer<Response>) invocationOnMock -> {
            assertEquals(request, invocationOnMock.getArgument(0));
            if (failNumber.getAndIncrement() < limit) {
                return buildResponse(503);
            }
            return buildResponse(200);
        });

        Response actualResponse = httpErrorRetrier.intercept(mockChain);
        assertEquals(200, actualResponse.code());
    }

    @Test
    public void testSleepMultiplier() throws IOException {
        final int limit = 3;
        final int sleepMultiplier = 2;
        HttpErrorRetrier httpErrorRetrier = new HttpErrorRetrier(Collections.singletonList(503), limit, sleepMultiplier);

        when(mockChain.proceed(any())).thenAnswer((Answer<Response>) invocationOnMock -> {
            assertEquals(request, invocationOnMock.getArgument(0));
            if (failNumber.getAndIncrement() < limit) {
                return buildResponse(503);
            }
            return buildResponse(200);
        });

        int expectedTotalSleepSeconds = 0;
        for (int i = 0; i < failNumber.get() - 1; i++) {
            expectedTotalSleepSeconds += Math.pow(sleepMultiplier, i);
        }

        long momentBeforeRetries = System.currentTimeMillis();
        Response actualResponse = httpErrorRetrier.intercept(mockChain);
        long momentAfterSuccess = System.currentTimeMillis();
        assertEquals(200, actualResponse.code());
        assertTrue(TimeUnit.SECONDS.toMillis(expectedTotalSleepSeconds) <= momentAfterSuccess - momentBeforeRetries);
    }

    @Test
    public void testFailure() throws IOException {
        final int limit = 2;
        final int sleepMultiplier = 1;
        HttpErrorRetrier httpErrorRetrier = new HttpErrorRetrier(Collections.singletonList(503), limit, sleepMultiplier);

        when(mockChain.proceed(any())).thenAnswer((Answer<Response>) invocationOnMock -> {
            assertEquals(request, invocationOnMock.getArgument(0));
            return buildResponse(503);
        });

        Response actualResponse = httpErrorRetrier.intercept(mockChain);
        assertEquals(503, actualResponse.code());
    }

    @Test
    public void retriesResponseWithBodyThroughRealChain() throws IOException {
        try (ClientAndServer server = startClientAndServer()) {
            server.when(request().withPath(PATH), Times.exactly(1))
                    .respond(response().withStatusCode(503).withBody("{\"message\":\"aggregator is not ready\"}"));
            server.when(request().withPath(PATH)).respond(response().withStatusCode(200).withBody("ready"));

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(new HttpErrorRetrier(Collections.singletonList(503), 3, 1))
                    .build();
            Request realRequest = new Request.Builder().get()
                    .url("http://localhost:" + server.getLocalPort() + PATH).build();

            try (Response response = client.newCall(realRequest).execute()) {
                assertEquals(200, response.code());
                assertEquals("ready", response.body().string());
            }
            server.verify(request().withPath(PATH), VerificationTimes.exactly(2));
        }
    }

    private Response buildResponse(int statusCode) {
        return new Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .message("test message")
                .code(statusCode)
                .body(ResponseBody.create(MediaType.get("application/json"), "{\"message\":\"aggregator\"}"))
                .build();
    }
}

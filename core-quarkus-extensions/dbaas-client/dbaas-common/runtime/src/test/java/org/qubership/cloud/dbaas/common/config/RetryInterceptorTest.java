package org.qubership.cloud.dbaas.common.config;

import okhttp3.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import static org.mockito.Mockito.*;

public class RetryInterceptorTest {
    private Interceptor.Chain chain;
    private Request request;
    private Response response;
    private RetryInterceptor interceptor;
    private static final String DB_URL  = "http://dbaas-agent:8080";
    private static final int MAX_RETRIES = 1;
    private static final long INITIAL_RETRY_DELAY = 100;

    @BeforeEach
    void setUp() {
        chain = Mockito.mock(Interceptor.Chain.class);
        request = new Request.Builder().url(DB_URL).build();
        response = new Response.Builder().request(request).protocol(Protocol.HTTP_1_1).code(200).message("success").build();
        Mockito.when(chain.request()).thenReturn(request);
        try {
            Mockito.when(chain.proceed(any(Request.class))).thenThrow(new java.net.SocketTimeoutException()).thenReturn(response);
        } catch (IOException e) {
            throw new RuntimeException("IOException occurred while mocking chain.proceed", e);
        }
    }
    @Test
    void testInterceptorRetriesAndSucceeds() throws IOException {
        interceptor = new RetryInterceptor(MAX_RETRIES, INITIAL_RETRY_DELAY);
        Response actualResponse = interceptor.intercept(chain);
        Assertions.assertEquals(actualResponse,response);
        verify(chain, times(2)).proceed(request);
    }
    @Test
    void testInterceptorExceedsMaxRetries() throws IOException {
        interceptor = new RetryInterceptor(MAX_RETRIES, INITIAL_RETRY_DELAY);
        //throw exception twice for exceeding retries
        Mockito.when(chain.proceed(any(Request.class))).thenThrow(new java.net.SocketTimeoutException())
                .thenThrow(new java.net.SocketTimeoutException());

        Exception thrownException = null;
        try {
            interceptor.intercept(chain);
        } catch (IOException e) {
            thrownException = e;
        }
        verify(chain, times(2)).proceed(request);
        Assertions.assertNotNull(thrownException, "An IOException should be thrown");
    }
}
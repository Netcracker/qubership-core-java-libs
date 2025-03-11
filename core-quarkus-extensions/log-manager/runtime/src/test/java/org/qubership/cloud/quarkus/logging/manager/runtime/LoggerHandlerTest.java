package org.qubership.cloud.quarkus.logging.manager.runtime;

import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

class LoggerHandlerTest {

    private LoggerHandler loggerHandler;
    private RoutingContext routingContext;
    private HttpServerRequest request;
    private HttpServerResponse response;
    private MultiMap headers;

    @BeforeEach
    void setUp() {
        loggerHandler = new LoggerHandler();
        routingContext = mock(RoutingContext.class);
        request = mock(HttpServerRequest.class);
        response = mock(HttpServerResponse.class);
        headers = mock(MultiMap.class);

        when(routingContext.request()).thenReturn(request);
        when(routingContext.response()).thenReturn(response);
        when(response.headers()).thenReturn(headers);
    }

    @Test
    void testHandleGet() {
        when(request.method()).thenReturn(HttpMethod.GET);

        loggerHandler.handle(routingContext);

        verify(headers).add("Content-Type", "application/json");
        verify(response).end(anyString());
    }

    @Test
    void testHandleNonGet() {
        when(request.method()).thenReturn(HttpMethod.POST);

        loggerHandler.handle(routingContext);

        verify(headers, never()).add(anyString(), anyString());
        verify(response).end();
    }
}

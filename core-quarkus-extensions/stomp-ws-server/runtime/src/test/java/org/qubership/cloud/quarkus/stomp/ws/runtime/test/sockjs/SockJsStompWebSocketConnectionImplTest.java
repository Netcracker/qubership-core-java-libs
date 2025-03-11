package org.qubership.cloud.quarkus.stomp.ws.runtime.test.sockjs;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.qubership.cloud.quarkus.stomp.ws.runtime.sockjs.SockJsStompWebSocketConnectionImpl;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.impl.VertxImpl;
import io.vertx.ext.stomp.DefaultSubscribeHandler;
import io.vertx.ext.stomp.ServerFrame;
import io.vertx.ext.stomp.StompServer;
import io.vertx.ext.stomp.StompServerConnection;
import io.vertx.ext.web.impl.ServerWebSocketWrapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SockJsStompWebSocketConnectionImplTest {

    ServerWebSocket socket;
    StompServer server;
    Handler<ServerFrame> writtenFrameHandler;
    Vertx vertx;
    ServerWebSocketWrapper serverWebSocketWrapper;

    @BeforeEach
    void setup() {
        socket = mock(ServerWebSocket.class);
        server = mock(StompServer.class);
        writtenFrameHandler = (Handler<ServerFrame>) mock(Handler.class);
        vertx = mock(VertxImpl.class);
        serverWebSocketWrapper = mock(ServerWebSocketWrapper.class);
    }

    @Test
    void configureHeartbeat() {
        Handler<StompServerConnection> pingHandler = (Handler<StompServerConnection>) mock(Handler.class);
        Handler<ServerFrame> writtenFrameHandler = new DefaultSubscribeHandler();
        SockJsStompWebSocketConnectionImpl sockJsStompWebSocketConnectionImpl = new SockJsStompWebSocketConnectionImpl(socket, server, writtenFrameHandler, vertx);

        Mockito.doAnswer(invocation -> {
            Long arg1 = invocation.getArgument(0);
            Assertions.assertEquals(25000, (long) arg1);

            Handler<Long> arg2 = invocation.getArgument(1);
            arg2.handle(12L);

            return arg1;
        }).when(vertx).setPeriodic(anyLong(), Mockito.any());

        Mockito.when(socket.isClosed()).thenReturn(false);
        sockJsStompWebSocketConnectionImpl.configureHeartbeat(11656, 217672, pingHandler);
        Mockito.verify(socket).writeTextMessage("h");

        Mockito.when(socket.isClosed()).thenReturn(true);
        sockJsStompWebSocketConnectionImpl.configureHeartbeat(11656, 217672, pingHandler);
        Mockito.verify(vertx).cancelTimer(12L);
    }

    @Test
    void writeMessagesInSocket() {
        Future<Void> future = (Future<Void>) mock(Future.class);
        String message = "a[\"\"]";
        when(socket.writeTextMessage(message)).thenReturn(future);
        SockJsStompWebSocketConnectionImpl SockJsStompWebSocketConnectionImpl = new SockJsStompWebSocketConnectionImpl(socket, server, writtenFrameHandler, vertx);
        SockJsStompWebSocketConnectionImpl.write(Buffer.buffer());
        Mockito.doAnswer(invocation -> {
            Assertions.assertEquals(message, invocation.getArgument(0));
            return null;
        }).when(socket).writeTextMessage(any());
    }

    @Test
    void writeMessagesInSocketWithNonEmptyBuffer() throws JsonProcessingException {
        Future<Void> future = (Future<Void>) mock(Future.class);
        String value = "\"10\"";
        String message = "a" + "[" + value + "]";
        Buffer buffer = Buffer.buffer(new ObjectMapper().writeValueAsBytes(10));
        when(socket.writeTextMessage(message)).thenReturn(future);
        SockJsStompWebSocketConnectionImpl SockJsStompWebSocketConnectionImpl = new SockJsStompWebSocketConnectionImpl(socket, server, writtenFrameHandler, vertx);
        Mockito.doAnswer(invocation -> {
            Assertions.assertEquals(message, invocation.getArgument(0));
            return null;
        }).when(socket).writeTextMessage(any());
        SockJsStompWebSocketConnectionImpl.write(buffer);
    }
}

package org.qubership.cloud.quarkus.stomp.ws.runtime.test;

import org.qubership.cloud.quarkus.stomp.ws.runtime.StompServerRecorder;
import org.qubership.cloud.quarkus.stomp.ws.runtime.sockjs.SockJsStompServer;
import io.quarkus.test.junit.QuarkusTest;
import io.vertx.core.Vertx;
import io.vertx.ext.stomp.StompServer;
import io.vertx.ext.stomp.impl.StompServerImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

@QuarkusTest
class StompServerRecorderTest {

    private StompServer stompServer;
    private final String websocketPath = "/stomp";

    @Test
    void initializeStompServer() throws NoSuchFieldException, IllegalAccessException {
        boolean isSockJs = true;
        stompServer = getStompServer(isSockJs);
        if (!(stompServer instanceof SockJsStompServer)) {
            Assertions.fail("wrong StompServer when isSockjs is true");
        }
        Assertions.assertEquals(stompServer.options().getPort(), -1);
        Assertions.assertFalse(stompServer.options().isSecured());
        Assertions.assertEquals(stompServer.options().getWebsocketPath(), websocketPath);
        Assertions.assertTrue(stompServer.options().isWebsocketBridge());
    }

    @Test
    void initializeStompServerWhenSockJsFalse() throws NoSuchFieldException, IllegalAccessException {
        boolean isSockJs = false;
        stompServer = getStompServer(isSockJs);
        if (!(stompServer instanceof StompServerImpl)) {
            Assertions.fail("wrong StompServer when isSockjs is false");
        }
        Assertions.assertEquals(stompServer.options().getPort(), -1);
        Assertions.assertFalse(stompServer.options().isSecured());
        Assertions.assertEquals(stompServer.options().getWebsocketPath(), websocketPath);
        Assertions.assertTrue(stompServer.options().isWebsocketBridge());
    }

    private StompServer getStompServer(boolean isSockJs) throws NoSuchFieldException, IllegalAccessException {
        StompServerRecorder stompServerRecorder = new StompServerRecorder();
        stompServerRecorder.initStompServer(() -> Vertx.vertx(), websocketPath, isSockJs);
        Field field = StompServerRecorder.class.getDeclaredField("stompServer");
        field.setAccessible(true);
        stompServer = (StompServer) field.get(stompServerRecorder);
        return stompServer;
    }
}

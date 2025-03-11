package org.qubership.cloud.quarkus.stomp.ws.runtime.sockjs;

import com.fasterxml.jackson.core.io.JsonStringEncoder;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.ext.stomp.ServerFrame;
import io.vertx.ext.stomp.StompServer;
import io.vertx.ext.stomp.StompServerConnection;
import io.vertx.ext.stomp.impl.StompServerWebSocketConnectionImpl;
import org.jboss.logging.Logger;

public class SockJsStompWebSocketConnectionImpl extends StompServerWebSocketConnectionImpl {

    private final ServerWebSocket socket;
    private Vertx vertx;

    private static final Logger log = Logger.getLogger(SockJsStompWebSocketConnectionImpl.class.getName());

    public SockJsStompWebSocketConnectionImpl(ServerWebSocket socket,
                                              StompServer server,
                                              Handler<ServerFrame> writtenFrameHandler,
                                              Vertx vertx) {
        super(socket, server, writtenFrameHandler);
        this.socket = socket;
        this.vertx = vertx;
    }

    @Override
    public synchronized void configureHeartbeat(long ping, long pong, Handler<StompServerConnection> pingHandler) {
        vertx.setPeriodic(25_000, tid -> {
            if (!socket.isClosed()){
                socket.writeTextMessage("h");
            } else {
                vertx.cancelTimer(tid);
            }
        });
    }

    @Override
    public StompServerConnection write(Buffer buffer) {
        String message = buffer.toString();
        log.debugf("send message (before encoding): " + message);
        message = encode(message);
        log.debugf("send message (after encoding): " + message);
        socket.writeTextMessage(message);
        return this;
    }

    public String encode(String... messages) {
        StringBuilder sb = new StringBuilder();
        sb.append("a[");
        for (int i = 0; i < messages.length; i++) {
            sb.append('"');
            char[] quotedChars = applyJsonQuoting(messages[i]);
            sb.append(escapeSockJsSpecialChars(quotedChars));
            sb.append('"');
            if (i < messages.length - 1) {
                sb.append(',');
            }
        }
        sb.append(']');
        return sb.toString();
    }

    private boolean isSockJsSpecialChar(char ch) {
        return (ch <= '\u001F') || (ch >= '\u200C' && ch <= '\u200F') ||
                (ch >= '\u2028' && ch <= '\u202F') || (ch >= '\u2060' && ch <= '\u206F') ||
                (ch >= '\uFFF0') || (ch >= '\uD800' && ch <= '\uDFFF');
    }

    private String escapeSockJsSpecialChars(char[] characters) {
        StringBuilder result = new StringBuilder();
        for (char c : characters) {
            if (isSockJsSpecialChar(c)) {
                result.append('\\').append('u');
                String hex = Integer.toHexString(c).toLowerCase();
                for (int i = 0; i < (4 - hex.length()); i++) {
                    result.append('0');
                }
                result.append(hex);
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }

    protected char[] applyJsonQuoting(String content) {
        return JsonStringEncoder.getInstance().quoteAsString(content);
    }

}

package org.qubership.cloud.quarkus.stomp.ws.runtime.sockjs;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.net.NetServer;
import io.vertx.ext.stomp.Frames;
import io.vertx.ext.stomp.ServerFrame;
import io.vertx.ext.stomp.StompServer;
import io.vertx.ext.stomp.StompServerConnection;
import io.vertx.ext.stomp.StompServerHandler;
import io.vertx.ext.stomp.StompServerOptions;
import io.vertx.ext.stomp.impl.FrameParser;
import io.vertx.ext.stomp.impl.ServerFrameImpl;
import io.vertx.ext.stomp.impl.StompServerImpl;
import io.vertx.ext.stomp.impl.StompServerTCPConnectionImpl;

import java.util.Objects;

/** The code of this class is a copy of StompServerImpl with small changes **/
public class SockJsStompServer implements StompServer {
    private static final Logger LOGGER = LoggerFactory.getLogger(StompServerImpl.class);

    private final Vertx vertx;
    private final StompServerOptions options;
    private final NetServer server;

    private StompServerHandler handler;
    private volatile boolean listening;

    private Handler<ServerFrame> writingFrameHandler;

    /**
     * Creates a new instance of {@link StompServerImpl}.
     *
     * @param vertx   the vert.x instance
     * @param options the options
     */
    public SockJsStompServer(Vertx vertx, StompServerOptions options) {
        Objects.requireNonNull(vertx);
        Objects.requireNonNull(options);
        this.options = options;
        this.vertx = vertx;
        server = vertx.createNetServer(options);
    }

    @Override
    public synchronized StompServer handler(StompServerHandler handler) {
        Objects.requireNonNull(handler);
        this.handler = handler;
        return this;
    }

    @Override
    public Future<StompServer> listen() {
        Promise<StompServer> promise = Promise.promise();
        this.listen(promise);
        return promise.future();
    }

    @Override
    public StompServer listen(Handler<AsyncResult<StompServer>> handler) {
        return listen(options.getPort(), options.getHost(), handler);
    }

    @Override
    public Future<StompServer> listen(int port) {
        return listen(port, StompServerOptions.DEFAULT_STOMP_HOST);
    }

    @Override
    public Future<StompServer> listen(int port, String host) {
        Promise<StompServer> promise = Promise.promise();
        listen(port, host, promise);
        return promise.future();
    }

    @Override
    public StompServer listen(int port, Handler<AsyncResult<StompServer>> handler) {
        return listen(port, StompServerOptions.DEFAULT_STOMP_HOST, handler);
    }

    @Override
    public StompServer listen(int port, String host, Handler<AsyncResult<StompServer>> handler) {
        if (port == -1) {
            handler.handle(Future.failedFuture("TCP server disabled. The port is set to '-1'."));
            return this;
        }
        StompServerHandler stomp;
        synchronized (this) {
            stomp = this.handler;
        }

        Objects.requireNonNull(stomp, "Cannot open STOMP server - no StompServerConnectionHandler attached to the " +
                "server.");
        server
                .connectHandler(socket -> {
                    StompServerConnection connection = new StompServerTCPConnectionImpl(socket, this, writingFrameHandler);
                    FrameParser parser = new FrameParser(options);
                    socket.exceptionHandler((exception) -> {
                        LOGGER.error("The STOMP server caught a TCP socket error - closing connection", exception);
                        connection.close();
                    });
                    socket.endHandler(v -> connection.close());
                    parser
                            .errorHandler((exception) -> {
                                        connection.write(
                                                Frames.createInvalidFrameErrorFrame(exception));
                                        connection.close();
                                    }
                            )
                            .handler(frame -> stomp.handle(new ServerFrameImpl(frame, connection)));
                    socket.handler(parser);
                })
                .listen(port, host, ar -> {
                    if (ar.failed()) {
                        if (handler != null) {
                            vertx.runOnContext(v -> handler.handle(Future.failedFuture(ar.cause())));
                        } else {
                            LOGGER.error(ar.cause());
                        }
                    } else {
                        listening = true;
                        LOGGER.info("STOMP server listening on " + ar.result().actualPort());
                        if (handler != null) {
                            vertx.runOnContext(v -> handler.handle(Future.succeededFuture(this)));
                        }
                    }
                });
        return this;
    }

    @Override
    public Future<Void> close() {
        Promise<Void> promise = Promise.promise();
        this.close(promise);
        return promise.future();
    }

    @Override
    public boolean isListening() {
        return listening;
    }

    @Override
    public int actualPort() {
        return server.actualPort();
    }

    @Override
    public StompServerOptions options() {
        return options;
    }

    @Override
    public Vertx vertx() {
        return vertx;
    }

    @Override
    public synchronized StompServerHandler stompHandler() {
        return handler;
    }


    @Override
    public void close(Handler<AsyncResult<Void>> done) {
        if (!isListening()) {
            if (done != null) {
                vertx.runOnContext((v) -> done.handle(Future.succeededFuture()));
            }
            return;
        }

        Handler<AsyncResult<Void>> listener = (v) -> {
            if (v.succeeded()) {
                LOGGER.info("STOMP Server stopped");
            } else {
                LOGGER.info("STOMP Server failed to stop", v.cause());
            }

            listening = false;
            if (done != null) {
                done.handle(v);
            }
        };

        server.close(listener);
    }

    @Override
    public Handler<ServerWebSocket> webSocketHandler() {
        if (!options.isWebsocketBridge()) {
            return null;
        }

        StompServerHandler stomp;
        synchronized (this) {
            stomp = this.handler;
        }

        return socket -> {
            sendOpenFrame(socket);
            StompServerConnection connection = new SockJsStompWebSocketConnectionImpl(socket, this, writingFrameHandler, vertx);
            FrameParser parser = new SockJsFrameParser(options);
            socket.exceptionHandler((exception) -> {
                LOGGER.error("The STOMP server caught a WebSocket error - closing connection", exception);
                connection.close();
            });
            socket.endHandler(v -> connection.close());
            parser
                    .errorHandler((exception) -> {
                                connection.write(
                                        Frames.createInvalidFrameErrorFrame(exception));
                                connection.close();
                            }
                    )
                    .handler(frame -> stomp.handle(new ServerFrameImpl(frame, connection)));
            socket.handler(parser);
        };
    }

    private void sendOpenFrame(ServerWebSocket socket) {
        socket.writeTextMessage("o");
    }

    @Override
    public StompServer writingFrameHandler(Handler<ServerFrame> handler) {
        synchronized (this) {
            this.writingFrameHandler = handler;
        }
        return this;
    }
}

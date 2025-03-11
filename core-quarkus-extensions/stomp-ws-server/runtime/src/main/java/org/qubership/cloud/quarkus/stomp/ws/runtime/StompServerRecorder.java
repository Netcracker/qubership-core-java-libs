package org.qubership.cloud.quarkus.stomp.ws.runtime;

import org.qubership.cloud.quarkus.stomp.ws.runtime.handlers.SubscribeHandler;
import org.qubership.cloud.quarkus.stomp.ws.runtime.interceptors.SubscribeInterceptor;
import org.qubership.cloud.quarkus.stomp.ws.runtime.sockjs.SockJsStompServer;
import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.runtime.annotations.Recorder;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.ext.stomp.Destination;
import io.vertx.ext.stomp.DestinationFactory;
import io.vertx.ext.stomp.StompServer;
import io.vertx.ext.stomp.StompServerHandler;
import io.vertx.ext.stomp.StompServerOptions;
import io.vertx.ext.web.RoutingContext;
import jakarta.enterprise.inject.spi.CDI;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Recorder
public class StompServerRecorder {

    private static final Logger log = Logger.getLogger(StompServerRecorder.class.getName());

    private StompServer stompServer;

    List<DestinationProvider> destinationProviders;

    public void initStompServer(Supplier<Vertx> vertxSupplier, String websocketPath, boolean isSockJs) {
        collectDestinationProviders();
        SubscribeHandler subscribeHandler = getSubscribeHandler();
        Vertx vertx = vertxSupplier.get();
        StompServerOptions options = new StompServerOptions()
                .setPort(-1)
                .setSecured(false)
                .setWebsocketBridge(true)
                .setWebsocketPath(websocketPath);
        log.debugf("SockJS flag is %s", isSockJs);
        stompServer = isSockJs ? new SockJsStompServer(vertx, options) : StompServer.create(vertx, options);
        stompServer.handler(StompServerHandler.create(vertx)
                .destinationFactory(getDestinationFactory(vertx))
                .subscribeHandler(subscribeHandler));
    }

    private SubscribeHandler getSubscribeHandler() {
        InstanceHandle<SubscribeInterceptor> instance = Arc.container().instance(SubscribeInterceptor.class);
        if (instance.isAvailable()) {
            return new SubscribeHandler(instance.get());
        }
        return new SubscribeHandler();
    }

    private DestinationFactory getDestinationFactory(Vertx vertx) {
        return (v, name) -> {
            log.debugf("Destination path is %s", name);
            Optional<DestinationProvider> optionalDestinationProvider = destinationProviders.stream()
                    .filter(destinationProvider -> name.startsWith(destinationProvider.getDestinationPath()))
                    .findFirst();
            return optionalDestinationProvider
                    .map(destinationProvider -> {
                        log.debugf("Get known destination path");
                        return getDestination(vertx, name, destinationProvider);
                    })
                    .orElseGet(() -> {
                        log.debugf("Get unknown destination path");
                        return null;
                    });
        };
    }

    private Destination getDestination(Vertx vertx, String name, DestinationProvider destinationProvider) {
        if (!destinationProvider.isDestinationSetUp()) {
            if (destinationProvider.getDestinationType() == DestinationProvider.DestinationType.TOPIC) {
                log.debugf("Set subscription as TOPIC");
                destinationProvider.setDestination(Destination.topic(vertx, name));
            } else if (destinationProvider.getDestinationType() == DestinationProvider.DestinationType.QUEUE) {
                log.debugf("Set subscription as QUEUE");
                destinationProvider.setDestination(Destination.queue(vertx, name));
            } else {
                throw new RuntimeException("You must set destination type from Destination "
                        + destinationProvider.getDestinationPath());
            }
        }
        return destinationProvider.getDestination();
    }

    private void collectDestinationProviders() {
        destinationProviders = CDI.current()
                .select(DestinationProvider.class)
                .stream()
                .collect(Collectors.toList());
        log.debugf("%s implementations of destination provider have been found", destinationProviders.size());
    }

    public Handler<RoutingContext> stompHandler() {
        return routingContext -> {
            Future<ServerWebSocket> serverWebSocketFuture = routingContext.request().toWebSocket();
            serverWebSocketFuture.onComplete(event -> {
                if (event.succeeded()) {
                    ServerWebSocket webSocket = event.result();
                    Handler<ServerWebSocket> serverWebSocketHandler = stompServer.webSocketHandler();
                    serverWebSocketHandler.handle(webSocket);
                } else {
                    try {
                        throw event.cause();
                    } catch (Throwable throwable) {
                        throw new RuntimeException(throwable);
                    }
                }
            });
        };
    }

    public Handler<RoutingContext> sockJsInfoHandler() {
        return event -> {
            event.response().setStatusCode(200);
            event.response().end(Buffer.buffer("{\"websocket\":true}"));
        };
    }
}

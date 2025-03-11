package org.qubership.cloud.quarkus.logging.manager.runtime;

import io.quarkus.runtime.annotations.Recorder;
import io.vertx.core.Handler;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.RoutingContext;

import java.util.function.Consumer;

@Recorder
public class LoggerManagerRecorder {

    public Handler<RoutingContext> loggerHandler() {
        return new LoggerHandler();
    }

    public Consumer<Route> routeConsumer(Handler<RoutingContext> bodyHandler) {

        return new Consumer<Route>() {
            @Override
            public void accept(Route route) {
                route.handler(bodyHandler);
            }
        };

    }
}

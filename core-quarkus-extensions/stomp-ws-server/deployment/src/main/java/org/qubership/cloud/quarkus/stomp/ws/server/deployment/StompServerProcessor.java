package org.qubership.cloud.quarkus.stomp.ws.server.deployment;

import org.qubership.cloud.quarkus.stomp.ws.runtime.StompServerRecorder;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.vertx.core.deployment.CoreVertxBuildItem;
import io.quarkus.vertx.http.deployment.RouteBuildItem;

public class StompServerProcessor {

    private static final String FEATURE = "stomp-server";
    private final String infoPathSegment = "/info";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    StompServerConfig stompServerConfig;

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    public void configureStompWsServer(StompServerRecorder stompServerRecorder,
                                       BuildProducer<RouteBuildItem> routes,
                                       CoreVertxBuildItem vertx) {
        if (stompServerConfig.isSockJS) {
            configureStompServerOverSockJs(stompServerRecorder, routes, vertx);
        } else {
            configureStompServerOverWebsocket(stompServerRecorder, routes, vertx);
        }
    }

    private void configureStompServerOverSockJs(StompServerRecorder stompServerRecorder,
                                                BuildProducer<RouteBuildItem> routes,
                                                CoreVertxBuildItem vertx) {
        stompServerRecorder.initStompServer(vertx.getVertx(), stompServerConfig.websocketPath, true);
        routes.produce(RouteBuildItem.builder()
                .route(stompServerConfig.websocketPath + infoPathSegment)
                .handler(stompServerRecorder.sockJsInfoHandler())
                .build());
        routes.produce(RouteBuildItem.builder()
                .route(stompServerConfig.websocketPath + "/*")
                .handler(stompServerRecorder.stompHandler())
                .build());
    }

    private void configureStompServerOverWebsocket(StompServerRecorder stompServerRecorder,
                                                   BuildProducer<RouteBuildItem> routes,
                                                   CoreVertxBuildItem vertx) {
        stompServerRecorder.initStompServer(vertx.getVertx(), stompServerConfig.websocketPath, false);
        routes.produce(RouteBuildItem.builder()
                .route(stompServerConfig.websocketPath)
                .handler(stompServerRecorder.stompHandler())
                .build());
    }

}
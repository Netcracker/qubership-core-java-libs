package org.qubership.cloud.quarkus.logging.manager.deployment;

import org.qubership.cloud.quarkus.logging.manager.runtime.LoggerManagerRecorder;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.vertx.http.deployment.BodyHandlerBuildItem;
import io.quarkus.vertx.http.deployment.NonApplicationRootPathBuildItem;
import io.quarkus.vertx.http.deployment.RouteBuildItem;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;


class LoggingManagerProcessor {
    private static final String FEATURE = "logging-manager";
    private static final String GET_LOGGERS_LEVEL_PATH = "/api/logging/v1/levels";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @Record(ExecutionTime.RUNTIME_INIT)
    @BuildStep
    void includeRestEndpoints(BuildProducer<RouteBuildItem> routeProducer,
            NonApplicationRootPathBuildItem nonApplicationRootPathBuildItem,
            BodyHandlerBuildItem bodyHandlerBuildItem,
            LoggerManagerRecorder recorder) {
        Handler<RoutingContext> loggerHandler = recorder.loggerHandler();

        routeProducer.produce(nonApplicationRootPathBuildItem.routeBuilder()
                .management()
                .routeFunction( GET_LOGGERS_LEVEL_PATH,
                        recorder.routeConsumer(bodyHandlerBuildItem.getHandler()))
                .handler(loggerHandler)
                .build());
    }
}

package org.qubership.cloud.context.propagation.quarkus.runtime.configuration;

import org.qubership.cloud.context.propagation.quarkus.runtime.filter.QuarkusPreAuthnContextProviderHandler;
import io.quarkus.runtime.annotations.Recorder;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

@Recorder
public class QuarkusContextProvidersRecorder {

    public Handler<RoutingContext> preAuthnContextProviderHandler() {
        return new QuarkusPreAuthnContextProviderHandler();
    }

}

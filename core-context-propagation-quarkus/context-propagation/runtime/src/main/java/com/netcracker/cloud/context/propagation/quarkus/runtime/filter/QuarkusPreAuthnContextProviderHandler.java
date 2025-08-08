package com.netcracker.cloud.context.propagation.quarkus.runtime.filter;

import org.qubership.cloud.context.propagation.core.ContextInitializationStep;
import org.qubership.cloud.context.propagation.core.ContextManager;
import org.qubership.cloud.context.propagation.core.ContextProvider;
import org.qubership.cloud.context.propagation.core.RequestContextPropagation;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.stream.Collectors;

public class QuarkusPreAuthnContextProviderHandler implements Handler<RoutingContext> {

    private static final Logger log = LoggerFactory.getLogger(QuarkusPreAuthnContextProviderHandler.class);
    public static final String CONTEXT_PROPAGATION_CONTEXT_PRE_AUTHENTICATION = "context_propagation.context.pre_authentication";

    @Override
    public void handle(RoutingContext event) {
        log.trace("Init pre authentication contexts on incoming request");
        RequestContextPropagation.clear();
        RequestContextPropagation.initRequestContext(new VertxRequestContextData(event.request()), ContextInitializationStep.PRE_AUTHENTICATION);
        saveContext();
        event.next();
    }

    private void saveContext() {
        if (Vertx.currentContext() != null) {
            Set<String> contextNames = ContextManager.getContextProviders().stream()
                    .filter(contextProvider -> contextProvider.getInitializationStep() == ContextInitializationStep.PRE_AUTHENTICATION)
                    .map(ContextProvider::contextName)
                    .collect(Collectors.toSet());
            Vertx.currentContext().putLocal(CONTEXT_PROPAGATION_CONTEXT_PRE_AUTHENTICATION, ContextManager.createContextSnapshot(contextNames));
        }
    }
}

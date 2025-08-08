package com.netcracker.cloud.context.propagation.quarkus.runtime.filter;

import org.qubership.cloud.context.propagation.core.ContextInitializationStep;
import org.qubership.cloud.context.propagation.core.ContextManager;
import org.qubership.cloud.context.propagation.core.RequestContextPropagation;
import io.vertx.core.Vertx;
import jakarta.annotation.Priority;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static org.qubership.cloud.context.propagation.quarkus.runtime.filter.Priorities.CORE_CONTEXT_PROPAGATION_INCOMING_REQUEST;
import static org.qubership.cloud.context.propagation.quarkus.runtime.filter.QuarkusPreAuthnContextProviderHandler.CONTEXT_PROPAGATION_CONTEXT_PRE_AUTHENTICATION;

@Priority(CORE_CONTEXT_PROPAGATION_INCOMING_REQUEST)
public class QuarkusPostAuthnContextProviderFilter implements ContainerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(QuarkusPostAuthnContextProviderFilter.class);

    @Override
    public void filter(ContainerRequestContext requestContext) {
        log.trace("Init post authentication contexts on incoming request");
        activateContext();
        RequestContextPropagation.initRequestContext(new QuarkusRequestContextData(requestContext), ContextInitializationStep.POST_AUTHENTICATION);
    }

    private void activateContext() {
        if (Vertx.currentContext() != null) {
            Map<String, Object> contextSnapshot = Vertx.currentContext().getLocal(CONTEXT_PROPAGATION_CONTEXT_PRE_AUTHENTICATION);
            ContextManager.activateContextSnapshot(contextSnapshot);
        }
    }
}

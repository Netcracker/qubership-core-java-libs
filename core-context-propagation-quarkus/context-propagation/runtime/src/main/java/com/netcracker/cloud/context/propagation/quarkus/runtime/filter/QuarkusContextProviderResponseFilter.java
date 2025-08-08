package org.qubership.cloud.context.propagation.quarkus.runtime.filter;

import org.qubership.cloud.context.propagation.core.RequestContextPropagation;
import org.qubership.cloud.context.propagation.quarkus.runtime.interceptor.QuarkusResponseContextData;
import jakarta.annotation.Priority;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static org.qubership.cloud.context.propagation.quarkus.runtime.filter.Priorities.CORE_CONTEXT_PROPAGATION_OUTGOING_RESPONSE;

@Priority(CORE_CONTEXT_PROPAGATION_OUTGOING_RESPONSE)
public class QuarkusContextProviderResponseFilter implements ContainerResponseFilter {

    private static final Logger log = LoggerFactory.getLogger(QuarkusContextProviderResponseFilter.class);

    @Override
    public void filter(ContainerRequestContext containerRequestContext, ContainerResponseContext containerResponseContext) throws IOException {
        log.trace("Propagate response with headers");
        QuarkusResponseContextData responseContextData = new QuarkusResponseContextData();
        RequestContextPropagation.setResponsePropagatableData(responseContextData);
        responseContextData.addHeadersToMap(containerResponseContext.getHeaders());

        log.trace("Clear contexts");
        RequestContextPropagation.clear();
    }
}

package org.qubership.cloud.context.propagation.quarkus.runtime.filter;

import org.qubership.cloud.context.propagation.core.contextdata.IncomingContextData;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.MultivaluedMap;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class QuarkusRequestContextData implements IncomingContextData {

    private final ContainerRequestContext containerRequestContext;

    public QuarkusRequestContextData(ContainerRequestContext containerRequestContext) {
        this.containerRequestContext = containerRequestContext;
        containerRequestContext.getHeaders().putSingle("cloud-core.context-propagation.url", containerRequestContext.getUriInfo().getPath());
    }

    @Override
    public Object get(String name) {
        return containerRequestContext.getHeaders().getFirst(name);
    }

    @Override
    public Map<String, List<?>> getAll() {
        MultivaluedMap<String, String> headers = containerRequestContext.getHeaders();
        Map<String, List<?>> result = headers.entrySet()
                .stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue)
                );
        return result;
    }
}

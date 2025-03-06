package org.qubership.cloud.context.propagation.core;

import org.qubership.cloud.context.propagation.core.contextdata.IncomingContextData;
import org.qubership.cloud.context.propagation.core.contextdata.OutgoingContextData;
import org.qubership.cloud.context.propagation.core.contexts.ResponsePropagatableContext;
import org.qubership.cloud.context.propagation.core.contexts.SerializableContext;
import org.qubership.cloud.context.propagation.core.contexts.SerializableDataContext;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

/**
 * The class allows to init context based on request data and propagate context data to outgoing request.
 */
public class RequestContextPropagation implements AutoCloseable {

    public static void initRequestContext(IncomingContextData incomingContextData) {
        initRequestContext(incomingContextData, contextProvider -> true);
    }

    public static void initRequestContext(IncomingContextData incomingContextData, ContextInitializationStep initStep) {
        initRequestContext(incomingContextData, contextProvider -> contextProvider.getInitializationStep() == initStep);
    }

    private static void initRequestContext(IncomingContextData incomingContextData, Predicate<ContextProvider<?>> filter) {
        ContextManager.getContextProviders().stream()
                .filter(filter)
                .forEach(contextProvider -> {
                    Object contextValue = contextProvider.provide(incomingContextData);
                    if (contextValue != null) {
                        Strategy strategy = contextProvider.strategy();
                        strategy.set(contextValue);
                    }
                });
    }

    public static void populateResponse(OutgoingContextData outgoingContextData) {
        ContextManager.getAll()
                .stream()
                .filter(SerializableContext.class::isInstance)
                .map(SerializableContext.class::cast)
                .forEach(serializableContext -> serializableContext.serialize(outgoingContextData));
    }

    public static Set<String> getDownstreamHeaders() {
        Set<String> headers = new HashSet<>();
        ContextManager.getAll()
                .stream()
                .filter(SerializableDataContext.class::isInstance)
                .map(SerializableDataContext.class::cast)
                .forEach(serializableDataContext -> headers.addAll(serializableDataContext.getSerializableContextData().keySet()));
        return headers;
    }

    public static void setResponsePropagatableData(OutgoingContextData outgoingContextData) {
        ContextManager.getAll()
                .stream()
                .filter(ResponsePropagatableContext.class::isInstance)
                .map(ResponsePropagatableContext.class::cast)
                .forEach(responsePropagatableContext -> responsePropagatableContext.propagate(outgoingContextData));

    }

    public static void clear() {
        ContextManager.clearAll();
    }

    @Override
    public void close() {
        clear();
    }
}

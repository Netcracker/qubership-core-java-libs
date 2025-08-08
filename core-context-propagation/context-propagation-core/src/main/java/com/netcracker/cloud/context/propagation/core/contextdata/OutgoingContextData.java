package org.qubership.cloud.context.propagation.core.contextdata;

import org.qubership.cloud.context.propagation.core.ContextProvider;

/**
 * Implementation of the interface contains key:value data that should be propagated
 * in outgoing request (REST, messaging). <p>
 * <p>
 * In another microservice propagated data will be wrapped by {@link IncomingContextData}
 * and you can init your context using the method {@link ContextProvider#provide(IncomingContextData)}
 */
public interface OutgoingContextData {

    /**
     * Set context data that will be propagated in outgoing request
     *
     * @param name   Serialized key that will be put in request
     * @param values Serialized value
     */
    void set(String name, Object values);
}

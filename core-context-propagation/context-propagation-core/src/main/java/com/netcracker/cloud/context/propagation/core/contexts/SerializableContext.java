package org.qubership.cloud.context.propagation.core.contexts;

import org.qubership.cloud.context.propagation.core.contextdata.OutgoingContextData;

/**
 * If contextObject implements this interface then his data will be propagated in outgoing request, for example REST
 * or messaging
 */
public interface SerializableContext {
    /**
     * Implementation should populate the outgoing object according to data
     * that must be propagated in another service.
     * @param outgoingContextData contains propagated data
     */
    void serialize(OutgoingContextData outgoingContextData);
}

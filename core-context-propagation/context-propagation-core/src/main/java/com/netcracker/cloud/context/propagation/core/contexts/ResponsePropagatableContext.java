package com.netcracker.cloud.context.propagation.core.contexts;

import org.qubership.cloud.context.propagation.core.contextdata.OutgoingContextData;

/**
 * If contextObject implements this interface then his data will be propagated in outgoing response, for example REST
 * or messaging
 */
public interface ResponsePropagatableContext {
    void propagate(OutgoingContextData outgoingContextData);
}

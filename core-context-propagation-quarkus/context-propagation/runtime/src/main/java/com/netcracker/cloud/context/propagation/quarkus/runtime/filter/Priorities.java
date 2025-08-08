package org.qubership.cloud.context.propagation.quarkus.runtime.filter;

public class Priorities {
    public static final int CORE_CONTEXT_PROPAGATION_OUTGOING_RESPONSE = jakarta.ws.rs.Priorities.AUTHENTICATION - 1000;
    public static final int CORE_CONTEXT_PROPAGATION_INCOMING_REQUEST = jakarta.ws.rs.Priorities.AUTHORIZATION - 2;
}

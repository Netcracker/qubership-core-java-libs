package org.qubership.cloud.framework.contexts.tenant;


import org.qubership.cloud.context.propagation.core.contextdata.IncomingContextData;
import org.qubership.cloud.context.propagation.core.contextdata.OutgoingContextData;
import org.qubership.cloud.context.propagation.core.contexts.ResponsePropagatableContext;
import org.qubership.cloud.context.propagation.core.contexts.SerializableContext;
import org.qubership.cloud.context.propagation.core.contexts.SerializableDataContext;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Map;

@Getter
public class TenantContextObject implements SerializableContext, SerializableDataContext, ResponsePropagatableContext {
    private String tenant;
    public static final String TENANT_HEADER = "Tenant";

    public TenantContextObject(@Nullable String tenant) {
        this.tenant = tenant;
    }

    public TenantContextObject(@Nullable IncomingContextData contextData) {
        if (contextData != null && contextData.get(TENANT_HEADER) != null) {
            this.tenant = (String) contextData.get(TENANT_HEADER);
        }
    }

    @Override
    public void serialize(OutgoingContextData outgoingContextData) {
        if (tenant != null && !tenant.isEmpty()) {
            outgoingContextData.set(TENANT_HEADER, tenant);
        }
    }

    @Override
    public Map<String, Object> getSerializableContextData() {
        if (tenant != null && !tenant.isEmpty()) {
            return Map.of(TENANT_HEADER, tenant);
        }
        return Collections.emptyMap();
    }

    @Override
    public void propagate(OutgoingContextData outgoingContextData) {
        if (tenant!= null &&!tenant.isEmpty()) {
            outgoingContextData.set(TENANT_HEADER, tenant);
        }
    }
}
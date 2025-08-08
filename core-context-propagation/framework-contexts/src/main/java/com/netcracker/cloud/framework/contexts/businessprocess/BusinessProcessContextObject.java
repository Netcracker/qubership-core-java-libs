package org.qubership.cloud.framework.contexts.businessprocess;

import org.qubership.cloud.context.propagation.core.contextdata.IncomingContextData;
import org.qubership.cloud.context.propagation.core.contextdata.OutgoingContextData;
import org.qubership.cloud.context.propagation.core.contexts.ResponsePropagatableContext;
import org.qubership.cloud.context.propagation.core.contexts.SerializableContext;
import org.qubership.cloud.context.propagation.core.contexts.SerializableDataContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Map;

public class BusinessProcessContextObject implements SerializableContext,
        ResponsePropagatableContext, SerializableDataContext {

    public static final String BUSINESS_PROCESS_ID_SERIALIZATION_NAME = "Business-Process-Id";
    private static final Logger log = LoggerFactory.getLogger(BusinessProcessContextObject.class);
    private String businessProcessId;

    public BusinessProcessContextObject() {
    }

    public BusinessProcessContextObject(@NotNull String businessProcessId) {
        this.businessProcessId = businessProcessId;
    }

    public BusinessProcessContextObject(@Nullable IncomingContextData contextData) {
        if (contextData != null && contextData.get(BUSINESS_PROCESS_ID_SERIALIZATION_NAME) != null && !contextData.get(BUSINESS_PROCESS_ID_SERIALIZATION_NAME).equals("")) {
            this.businessProcessId = (String) contextData.get(BUSINESS_PROCESS_ID_SERIALIZATION_NAME);
        }
    }

    @Override
    public void serialize(OutgoingContextData contextData) {
        if (businessProcessId != null) {
            contextData.set(BUSINESS_PROCESS_ID_SERIALIZATION_NAME, businessProcessId);
        }
    }

    public String getBusinessProcessId() {
        return businessProcessId;
    }

    @Override
    public void propagate(OutgoingContextData outgoingContextData) {
        if (businessProcessId != null) {
            outgoingContextData.set(BUSINESS_PROCESS_ID_SERIALIZATION_NAME, businessProcessId);
        }
    }

    @Override
    public Map<String, Object> getSerializableContextData() {
        if (businessProcessId != null) {
            return Map.of(BUSINESS_PROCESS_ID_SERIALIZATION_NAME, businessProcessId);
        }
        return Collections.emptyMap();
    }
}

package com.netcracker.cloud.framework.contexts.originatingbiid;

import com.netcracker.cloud.context.propagation.core.contextdata.IncomingContextData;
import com.netcracker.cloud.context.propagation.core.contextdata.OutgoingContextData;
import com.netcracker.cloud.context.propagation.core.contexts.ResponsePropagatableContext;
import com.netcracker.cloud.context.propagation.core.contexts.SerializableContext;
import com.netcracker.cloud.context.propagation.core.contexts.SerializableDataContext;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Map;

public class OriginatingBiIdContextObject implements SerializableContext,
        ResponsePropagatableContext, SerializableDataContext {
    private String originatingBiId;

    public static final String ORIGINATING_BI_ID_SERIALIZATION_NAME = "originating-bi-id";

    public OriginatingBiIdContextObject(String originatingBiId) {
        this.originatingBiId = originatingBiId;
    }

    public OriginatingBiIdContextObject(@Nullable IncomingContextData contextData) {
        if (contextData != null && contextData.get(ORIGINATING_BI_ID_SERIALIZATION_NAME) != null) {
            this.originatingBiId = (String) contextData.get(ORIGINATING_BI_ID_SERIALIZATION_NAME);
        }
    }

    @Override
    public void serialize(OutgoingContextData contextData) {
        if (originatingBiId != null) {
            contextData.set(ORIGINATING_BI_ID_SERIALIZATION_NAME, originatingBiId);
        }
    }

    public String getOriginatingBiId() {
        return originatingBiId;
    }

    @Override
    public void propagate(OutgoingContextData outgoingContextData) {
        if (originatingBiId != null) {
            outgoingContextData.set(ORIGINATING_BI_ID_SERIALIZATION_NAME, originatingBiId);
        }
    }

    @Override
    public Map<String, Object> getSerializableContextData() {
        if (originatingBiId != null) {
            return Map.of(ORIGINATING_BI_ID_SERIALIZATION_NAME, originatingBiId);
        }
        return Collections.emptyMap();
    }
}

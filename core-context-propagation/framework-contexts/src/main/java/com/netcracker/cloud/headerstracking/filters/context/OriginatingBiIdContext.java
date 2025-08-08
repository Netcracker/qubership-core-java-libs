package com.netcracker.cloud.headerstracking.filters.context;

import com.netcracker.cloud.context.propagation.core.ContextManager;
import com.netcracker.cloud.framework.contexts.originatingbiid.OriginatingBiIdContextObject;

import static com.netcracker.cloud.framework.contexts.originatingbiid.OriginatingBiIdProvider.CONTEXT_NAME;

public class OriginatingBiIdContext {

    public static String get() {
        OriginatingBiIdContextObject originatingBiIdContextObject = ContextManager.get(CONTEXT_NAME);
        return originatingBiIdContextObject.getOriginatingBiId();
    }

    public static void set(String newOriginatingBiId) {
        OriginatingBiIdContextObject originatingBiIdContextObject = new OriginatingBiIdContextObject(newOriginatingBiId);
        ContextManager.set(CONTEXT_NAME, originatingBiIdContextObject);
    }

    public static void clear() {
        ContextManager.clear(CONTEXT_NAME);
    }

}

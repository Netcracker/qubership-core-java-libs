package com.netcracker.cloud.headerstracking.filters.context;

import org.qubership.cloud.context.propagation.core.ContextManager;
import org.qubership.cloud.framework.contexts.businessprocess.BusinessProcessContextObject;
import org.jetbrains.annotations.NotNull;

import static org.qubership.cloud.framework.contexts.businessprocess.BusinessProcessProvider.CONTEXT_NAME;

public class BusinessProcessIdContext {

    public static String get() {
        BusinessProcessContextObject businessProcessContextObject = ContextManager.get(CONTEXT_NAME);
        return businessProcessContextObject.getBusinessProcessId();
    }

    public static void set(@NotNull String newBusinessProcessId) {
        BusinessProcessContextObject businessProcessContextObject = new BusinessProcessContextObject(newBusinessProcessId);
        ContextManager.set(CONTEXT_NAME, businessProcessContextObject);
    }

    public static void clear() {
        ContextManager.clear(CONTEXT_NAME);
    }
}

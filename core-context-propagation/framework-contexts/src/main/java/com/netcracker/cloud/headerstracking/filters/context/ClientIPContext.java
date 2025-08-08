package com.netcracker.cloud.headerstracking.filters.context;

import com.netcracker.cloud.context.propagation.core.ContextManager;
import com.netcracker.cloud.framework.contexts.clientip.ClientIPContextObject;
import com.netcracker.cloud.framework.contexts.clientip.ClientIPProvider;

public class ClientIPContext {
    public static String get() {
        ClientIPContextObject clientIPContextObject = ContextManager.get(ClientIPProvider.CONTEXT_NAME);
        return clientIPContextObject.getClientIp();
    }

    public static void set(String clientIp) {
        ClientIPContextObject clientIPContextObject = new ClientIPContextObject(clientIp);
        ContextManager.set(ClientIPProvider.CONTEXT_NAME, clientIPContextObject);
    }

    public static void clear() {
        ContextManager.clear(ClientIPProvider.CONTEXT_NAME);
    }
}

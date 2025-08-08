package com.netcracker.cloud.headerstracking.filters.context;

import com.netcracker.cloud.context.propagation.core.ContextManager;
import com.netcracker.cloud.framework.contexts.xrequestid.XRequestIdContextObject;
import org.jetbrains.annotations.NotNull;

import static com.netcracker.cloud.framework.contexts.xrequestid.XRequestIdContextObject.X_REQUEST_ID;


public class RequestIdContext {

    public static String get() {
        XRequestIdContextObject xRequestIdContextObject = ContextManager.get(X_REQUEST_ID);
        return xRequestIdContextObject.getRequestId();
    }

    public static void set(@NotNull String newRequestId) {
        XRequestIdContextObject xRequestIdContextObject = new XRequestIdContextObject(newRequestId);
        ContextManager.set(X_REQUEST_ID, xRequestIdContextObject);
    }

    public static void clear() {
        ContextManager.clear(X_REQUEST_ID);
    }
}

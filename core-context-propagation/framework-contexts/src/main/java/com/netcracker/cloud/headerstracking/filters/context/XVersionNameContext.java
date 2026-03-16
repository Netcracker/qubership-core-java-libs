package com.netcracker.cloud.headerstracking.filters.context;

import com.netcracker.cloud.context.propagation.core.ContextManager;
import com.netcracker.cloud.framework.contexts.xversionname.XVersionNameContextObject;
import com.netcracker.cloud.framework.contexts.xversionname.XVersionNameProvider;


public class XVersionNameContext {
    public static String get() {
        XVersionNameContextObject xVersionNameContextObject = ContextManager.get(XVersionNameProvider.CONTEXT_NAME);
        return xVersionNameContextObject.getXVersionName();
    }

    public static void set(String xVersionName) {
        XVersionNameContextObject xVersionNameContextObject = new XVersionNameContextObject(xVersionName);
        ContextManager.set(XVersionNameProvider.CONTEXT_NAME, xVersionNameContextObject);
    }

    public static void clear() {
        ContextManager.clear(XVersionNameProvider.CONTEXT_NAME);
    }
}

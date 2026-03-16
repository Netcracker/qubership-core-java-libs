package com.netcracker.cloud.framework.contexts.helper;

import com.netcracker.cloud.context.propagation.core.ContextManager;

import java.lang.reflect.Field;
import java.util.Map;

public class ContextPropagationTestUtils {
    public static void reinitializeRegistry() {
        try {
            Field f = ContextManager.class.getDeclaredField("registry");
            f.setAccessible(true);
            ((Map) f.get(null)).clear();


            ContextManager.init();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

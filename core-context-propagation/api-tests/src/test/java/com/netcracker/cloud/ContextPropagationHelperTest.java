package com.netcracker.cloud;

import org.qubership.cloud.context.propagation.core.ContextManager;

import java.lang.reflect.Field;
import java.util.Map;

public class ContextPropagationHelperTest {

    public static void clearRegistry() {
        try {
            Field f = ContextManager.class.getDeclaredField("registry");
            f.setAccessible(true);
            ((Map) f.get(null)).clear();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

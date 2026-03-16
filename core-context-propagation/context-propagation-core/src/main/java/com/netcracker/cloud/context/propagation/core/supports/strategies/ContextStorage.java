package com.netcracker.cloud.context.propagation.core.supports.strategies;

import jakarta.ws.rs.BadRequestException;

import java.util.HashMap;
import java.util.Map;

public class ContextStorage {

    public interface CloseableContext extends AutoCloseable {
        @Override
        void close();
    }

    private static final ThreadLocalWithStack<Map<Class<?>, Object>> contextualData = new ThreadLocalWithStack<>();

    private static final int maxForwards = 20;

    public static <T> void pushContext(Class<T> type, T data) {
        getContextDataMap().put(type, data);
    }

    public static void pushContextDataMap(Map<Class<?>, Object> map) {
        contextualData.pushElement(map);
    }

    public static Map<Class<?>, Object> getContextDataMap() {
        return getContextDataMap(true);
    }

    public static <T> T getContextData(Class<T> type) {
        Map<Class<?>, Object> contextDataMap = getContextDataMap(false);
        if (contextDataMap == null) {
            return null;
        }
        return (T) contextDataMap.get(type);
    }

    public static <T> T popContextData(Class<T> type) {
        return (T) getContextDataMap().remove(type);
    }

    public static void clearContextData() {
        contextualData.clear();
    }

    public static Map<Class<?>, Object> getContextDataMap(boolean create) {
        // Pay special attention to NOT share this map across different threads
        Map<Class<?>, Object> map = contextualData.getLastElement();
        if (map == null && create) {
            contextualData.setLastElement(map = new HashMap<>());
        }
        return map;
    }

    public static Map<Class<?>, Object> addContextDataLevel() {
        if (getContextDataLevelCount() == maxForwards) {
            throw new BadRequestException("you have exceeded your maximum " + maxForwards + " allowed forwards");
        }
        Map<Class<?>, Object> map = new HashMap<>();
        contextualData.pushElement(map);
        return map;
    }

    public static CloseableContext addCloseableContextDataLevel() {
        addContextDataLevel();
        return () -> removeContextDataLevel();
    }

    public static CloseableContext addCloseableContextDataLevel(Map<Class<?>, Object> data) {
        pushContextDataMap(data);
        return () -> removeContextDataLevel();
    }

    public static int getContextDataLevelCount() {
        return contextualData.size();
    }

    public static void removeContextDataLevel() {
        contextualData.pullElement();
    }

    public static Object searchContextData(Object o) {
        for (int i = contextualData.size() - 1; i >= 0; i--) {
            Map<Class<?>, Object> map = contextualData.getElement(i);
            if (map.containsKey(o)) {
                return map.get(o);
            }
        }
        return null;
    }
}

package com.netcracker.cloud.context.propagation.core.contexts;

import java.util.Map;

/**
 * if the context is to be serializable then it must implement this interface.
 */
public interface SerializableDataContext {

    /**
     * @return method must return simple and primitive serializable data, such as string, int, bool
     * {@code List<String>} and so on. In deserialization phase, context will give data which were provided by this method.
     */
    Map<String, Object> getSerializableContextData();

}

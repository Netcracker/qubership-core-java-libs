package org.qubership.cloud.context.propagation.core.contextdata;

import java.util.List;
import java.util.Map;

/**
 * This is adapter between incoming request and contexts.
 * Implementation is initialized on each request and contains serialized context data.
 */
public interface IncomingContextData {
    /**
     * The method returns context data from request (REST and messaging).
     *
     * @param name serialized field name of context object
     * @return serialized value
     */
    Object get(String name);

    /**
     * The method returns all data that contain in an incoming request (REST or messaging).
     * <p>
     * Notice: The method is added for backward compatibility and will be deleted next major release.
     *
     * @return Map of request data, where key is header name and value is the header value.
     */
    @Deprecated
    Map<String, List<?>> getAll();
}

package com.netcracker.cloud.context.propagation.core.contexts;


/**
 *  If context object has default context value then it should implement this interface
 */
public interface DefaultValueAwareContext <V> {
    /**
     * @return Return default context object
     */
    V getDefault();
}

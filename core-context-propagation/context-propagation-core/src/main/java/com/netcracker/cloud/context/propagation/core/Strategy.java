package com.netcracker.cloud.context.propagation.core;

import java.util.Optional;

/**
 * Strategy describes how we store context object. For example, implementation can be based on
 * {@code ThreadLocal} or {@code InheritableThreadLocal} or whatever you want. Also the interface defines
 *  methods allowing get, set, clear context object from context.
 *  Notes: implementations must mimic it's logic to ThreadLocal interface logic
 *
 * @param <V> Context object
 */
public interface Strategy<V> {
    void clear();

    void set(V value);

    V get();

    Optional<V> getSafe();

}

package com.netcracker.cloud.context.propagation.core;

/**
 * {@link AutoCloseable} Scope to return previous Context state.
 * Is convenient to be used within try-with-resources statement
 * */
public interface Scope extends AutoCloseable {
    Scope NOOP_SCOPE = () -> {};

    @Override
    void close();
}

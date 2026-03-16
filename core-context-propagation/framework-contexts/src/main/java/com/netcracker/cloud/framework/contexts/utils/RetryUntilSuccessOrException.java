package com.netcracker.cloud.framework.contexts.utils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

public class RetryUntilSuccessOrException<T, E extends Throwable> extends RetryOperation<T> {
    private static final Logger log = LoggerFactory.getLogger(RetryUntilSuccessOrException.class);

    private final Class<E> expectedException;

    public RetryUntilSuccessOrException(@NotNull Supplier<T> action, @NotNull Class<E> expectedException) {
        super(null, action, null);
        this.expectedException = expectedException;
    }

    public RetryUntilSuccessOrException(@Nullable String operationName, @NotNull Supplier<T> action, @NotNull Class<E> expectedException) {
        super(operationName, action, null);
        this.expectedException = expectedException;
    }

    public RetryUntilSuccessOrException(@Nullable String operationName, @NotNull Supplier<T> action, @Nullable Supplier<Long> delaySupplier, @NotNull Class<E> expectedException) {
        super(operationName, action, delaySupplier);
        this.expectedException = expectedException;
    }

    public T perform() throws E, InterruptedException {
        for (;;) {
            try {
                log.debug("[{}] trying to perform operation", operationName);
                T result = action.get();
                log.debug("[{}] successfully got operation result: {}", operationName, result);
                return result;
            } catch (Throwable t) {
                if (expectedException.isInstance(t)) {
                    log.warn("[{}] got expected exception (will be rethrown): ", operationName, t);
                    throw t;
                }
                log.error("[{}] failed with exception (will retry after delay): ", operationName, t);
                sleepFailsafe();
            }
        }
    }
}

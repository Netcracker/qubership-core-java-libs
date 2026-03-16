package com.netcracker.cloud.framework.contexts.utils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

public class RetryUntilSuccess<T> extends RetryOperation<T> {
    private static final Logger log = LoggerFactory.getLogger(RetryUntilSuccess.class);

    public RetryUntilSuccess(@NotNull Supplier<T> action) {
        super(null, action, null);
    }

    public RetryUntilSuccess(@Nullable String operationName, @NotNull Supplier<T> action) {
        super(operationName, action, null);
    }

    public RetryUntilSuccess(@Nullable String operationName, @NotNull Supplier<T> action, @Nullable Supplier<Long> delaySupplier) {
        super(operationName, action, delaySupplier);
    }

    public T perform() throws InterruptedException {
        for (;;) {
            try {
                log.debug("[{}] trying to perform operation", operationName);
                final T result = action.get();
                log.debug("[{}] successfully got operation result: {}", operationName, result);
                return result;
            } catch (Exception e) {
                log.error("[{}] failed with exception (will retry after delay): ", operationName, e);
                sleepFailsafe();
            }
        }
    }
}

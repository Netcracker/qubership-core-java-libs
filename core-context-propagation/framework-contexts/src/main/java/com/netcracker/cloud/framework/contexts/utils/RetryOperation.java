package com.netcracker.cloud.framework.contexts.utils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.function.Supplier;

public abstract class RetryOperation<T> {
    private static final Logger log = LoggerFactory.getLogger(RetryOperation.class);

    protected final String operationName;
    protected final Supplier<T> action;
    protected final Supplier<Long> delaySupplier;

    public RetryOperation(@NotNull Supplier<T> action) {
        this(null, action, null);
    }

    public RetryOperation(@Nullable String operationName, @NotNull Supplier<T> action) {
        this(operationName, action, null);
    }

    public RetryOperation(@Nullable String operationName, @NotNull Supplier<T> action, @Nullable Supplier<Long> delaySupplier) {
        this.operationName = operationName == null ? UUID.randomUUID().toString() : operationName;
        this.action = action;
        this.delaySupplier = delaySupplier == null ? defaultDelaySupplier() : delaySupplier;
    }

    private static Supplier<Long> defaultDelaySupplier() {
        return () -> 500L;
    }

    protected void sleepFailsafe() throws InterruptedException {
        try {
            final Long nextDelayMillis = delaySupplier.get();
            if (nextDelayMillis > 0) {
                Thread.sleep(nextDelayMillis);
            }
        } catch (InterruptedException e) {
            log.error("[{}] Sleeping interrupted thread before next retry: ", operationName, e);
            throw e;
        } catch (Exception e) {
            log.error("[{}] Error in sleeping thread before next retry: ", operationName, e);
        }
    }
}

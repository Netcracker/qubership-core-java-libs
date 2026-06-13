package com.netcracker.cloud.podsecrets;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * Thread-safe TTL cache for a single value.
 * On expiry, the first caller triggers a synchronous reload.
 */
public class CacheableValue<T> {
    private final Duration ttl;
    private final Supplier<T> refresher;
    private final AtomicReference<T> value = new AtomicReference<>();
    private final AtomicLong expiredAt = new AtomicLong(0);
    private final Supplier<Long> timeProvider;

    public CacheableValue(Duration ttl, Supplier<T> refresher) {
        this(ttl, refresher, System::currentTimeMillis);
    }

    CacheableValue(Duration ttl, Supplier<T> refresher, Supplier<Long> timeProvider) {
        this.ttl = ttl;
        this.refresher = refresher;
        this.timeProvider = timeProvider;
    }

    public T get() {
        if (expiredAt.get() <= timeProvider.get()) {
            synchronized (this) {
                if (expiredAt.get() <= timeProvider.get()) {
                    value.set(refresher.get());
                    expiredAt.set(timeProvider.get() + ttl.toMillis());
                }
            }
        }
        return value.get();
    }
}

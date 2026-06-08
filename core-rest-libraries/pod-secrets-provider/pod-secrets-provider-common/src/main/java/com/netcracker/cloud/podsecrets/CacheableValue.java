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

    public CacheableValue(Duration ttl, Supplier<T> refresher) {
        this.ttl = ttl;
        this.refresher = refresher;
    }

    public T get() {
        if (expiredAt.get() <= System.currentTimeMillis()) {
            synchronized (this) {
                // double-check inside lock
                if (expiredAt.get() <= System.currentTimeMillis()) {
                    value.set(refresher.get());
                    expiredAt.set(System.currentTimeMillis() + ttl.toMillis());
                }
            }
        }
        return value.get();
    }
}

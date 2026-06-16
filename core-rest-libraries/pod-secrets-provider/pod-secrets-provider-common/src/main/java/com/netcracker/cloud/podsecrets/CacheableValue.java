package com.netcracker.cloud.podsecrets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * Thread-safe TTL cache for a single value.
 * On expiry, the first caller triggers a synchronous reload.
 * If the reload throws a {@link RuntimeException} and a previously cached value exists,
 * the stale value is returned and the TTL is extended — preventing a blank result on transient errors.
 */
public class CacheableValue<T> {
    private static final Logger log = LoggerFactory.getLogger(CacheableValue.class);

    private final Duration ttl;
    private final Supplier<T> refresher;
    private final AtomicReference<T> value = new AtomicReference<>();
    private final AtomicLong expiredAt = new AtomicLong(0);
    private final Supplier<Long> timeProvider;

    public CacheableValue(Duration ttl, Supplier<T> refresher) {
        this(ttl, refresher, null, System::currentTimeMillis);
    }

    public CacheableValue(Duration ttl, Supplier<T> refresher, T defaultValue) {
        this(ttl, refresher, defaultValue, System::currentTimeMillis);
    }

    CacheableValue(Duration ttl, Supplier<T> refresher, T defaultValue, Supplier<Long> timeProvider) {
        this.ttl = ttl;
        this.refresher = refresher;
        this.value.set(defaultValue);
        this.timeProvider = timeProvider;
    }

    public T get() {
        if (expiredAt.get() <= timeProvider.get()) {
            synchronized (this) {
                if (expiredAt.get() <= timeProvider.get()) {
                    try {
                        value.set(refresher.get());
                    } catch (RuntimeException e) {
                        log.warn("Reload failed, returning stale value");
                    }
                    expiredAt.set(timeProvider.get() + ttl.toMillis());
                }
            }
        }
        return value.get();
    }
}

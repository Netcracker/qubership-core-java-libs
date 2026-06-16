package com.netcracker.cloud.podsecrets;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

class CacheableValueTest {
    @Test
    void getWithRefresh() {
        var counter = new AtomicInteger(0);
        var timeProvider = new AtomicLong(System.currentTimeMillis());
        var cv = new CacheableValue<>(Duration.ofMillis(100), counter::incrementAndGet, null, timeProvider::get);
        assertEquals(1, cv.get());
        assertEquals(1, cv.get()); // get cached value

        // shift time machine in future
        timeProvider.addAndGet(Duration.ofMillis(1010).toMillis());
        // reload should happen inside
        assertEquals(2, cv.get());
        assertEquals(2, cv.get()); // cached
    }

    @Test
    void getReturnsStaleValueOnReloadFailure() {
        var timeProvider = new AtomicLong(System.currentTimeMillis());
        var failNext = new AtomicInteger(0);
        var cv = new CacheableValue<>(Duration.ofMillis(100), () -> {
            if (failNext.get() == 1) throw new RuntimeException("transient error");
            return "fresh";
        }, null, timeProvider::get);

        assertEquals("fresh", cv.get());

        // mark next reload to fail and advance time
        failNext.set(1);
        timeProvider.addAndGet(Duration.ofMillis(1010).toMillis());

        // should return stale value
        assertEquals("fresh", cv.get());
    }

    @Test
    void getReturnsDefaultOnFirstLoadFailure() {
        var cv = new CacheableValue<>(Duration.ofMillis(100), () -> {
            throw new RuntimeException("initial failure");
        }, "default");

        assertEquals("default", cv.get());
    }

}

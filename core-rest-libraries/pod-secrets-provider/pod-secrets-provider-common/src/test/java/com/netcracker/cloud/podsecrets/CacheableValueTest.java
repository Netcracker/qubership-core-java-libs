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
        var cv = new CacheableValue<>(Duration.ofMillis(100), counter::incrementAndGet, timeProvider::get);
        assertEquals(1, cv.get());
        assertEquals(1, cv.get()); // get cached value

        // shift time machine in future
        timeProvider.addAndGet(Duration.ofMillis(1010).toMillis());
        // reload should happen inside
        assertEquals(2, cv.get());
        assertEquals(2, cv.get()); // cached
    }

}

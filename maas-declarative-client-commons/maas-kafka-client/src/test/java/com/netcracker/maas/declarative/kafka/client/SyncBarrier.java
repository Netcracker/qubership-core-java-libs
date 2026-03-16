package com.netcracker.maas.declarative.kafka.client;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.fail;

@Slf4j
public class SyncBarrier {

    private final ConcurrentMap<String, Semaphore> events = new ConcurrentHashMap<>();

    public void notify(String eventName) {
        log.info("Notify: {}", eventName);
        events.computeIfAbsent(eventName, k -> new Semaphore(0)).release();
    }

    @SneakyThrows
    public void await(String eventName, Duration timeout) {
        log.info("Await: {} (timeout: {})", eventName, timeout);
        var sem = events.computeIfAbsent(eventName, k -> new Semaphore(0));
        if (!sem.tryAcquire(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
            fail("timeout waiting event: " + eventName);
        }
        log.info("Event received: {}", eventName);
    }

    public void reset() {
        events.clear();
    }
}

package com.netcracker.maas.declarative.kafka.client;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

@Slf4j
public class SyncBarrier {
    private final BlockingDeque<String> queue = new LinkedBlockingDeque<>();

    public void notify(String eventName) {
        log.info("Notify: {}", eventName);
        queue.addLast(eventName);
    }

    @SneakyThrows
    public void await(String eventName, Duration timeout) {
        log.info("Await: {} (timeout: {})", eventName, timeout);
        var ev = queue.poll(timeout.toMillis(), TimeUnit.MILLISECONDS);
        if (ev == null) {
            fail("timeout waiting event: " + eventName);
        }
        assertEquals(eventName, ev, "received event `" + ev + "' differs from expected: " + eventName);
        log.info("Event received: {}", ev);
    }
}

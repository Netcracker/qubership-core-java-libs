package com.netcracker.maas.declarative.kafka.client.impl.client.consumer.executor;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AwaitExecutorServiceTest {
    @Test
    void testAwaitList() {
        var list = new AwaitExecutorService(List.of(10L, 20L));
        assertEquals(0, list.getTimeAwaitValue());
        assertEquals(0, list.getTimeAwaitValue());
        list.incrementInterval();
        assertEquals(10, list.getTimeAwaitValue());
        assertEquals(10, list.getTimeAwaitValue());
        list.incrementInterval();
        assertEquals(20, list.getTimeAwaitValue());
        assertEquals(20, list.getTimeAwaitValue());
        list.incrementInterval(); // test limit
        assertEquals(20, list.getTimeAwaitValue());
        list.resetAwaitTimeValues();
        assertEquals(0, list.getTimeAwaitValue());
    }

    @Test
    void testIncorrectArgs() {
        assertThrows(IllegalArgumentException.class, () -> new AwaitExecutorService(List.of()));
    }
}

package com.netcracker.maas.declarative.kafka.client.impl.client.consumer.filter.impl;

import org.qubership.cloud.maas.bluegreen.kafka.Record;
import org.qubership.maas.declarative.kafka.client.api.filter.ConsumerRecordFilter;
import org.qubership.maas.declarative.kafka.client.impl.client.consumer.filter.Chain;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConsumerFilterExecutorTest {

    @Test
    void execute() {
        AtomicReference<String> result = new AtomicReference<>("");
        FilterExecutor.execute(
                List.of(
                        new TestRecordFilter("hello", result),
                        new TestRecordFilter(" ", result),
                        new TestRecordFilter("world", result)
                ),
                null);
        assertEquals("hello world", result.get());
    }

    static class TestRecordFilter implements ConsumerRecordFilter {
        private final String addition;
        private final AtomicReference<String> result;

        public TestRecordFilter(String addition, AtomicReference<String> result) {
            this.addition = addition;
            this.result = result;
        }

        @Override
        public void doFilter(Record<?, ?> record, Chain<Record<?, ?>> next) {
            result.set(result.get() + addition);
            if (next != null) {
                next.doFilter(record);
            }
        }

        @Override
        public int order() {
            return 1;
        }
    }
}


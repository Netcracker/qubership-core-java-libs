package com.netcracker.maas.declarative.kafka.client.impl.client.consumer.executor;

import java.util.List;
import java.util.Objects;

// Not thread safe
public class AwaitExecutorService {
    private final List<Long> awaitValues;
    private int pos;

    public AwaitExecutorService(List<Long> awaitValues) {
        if (awaitValues == null || awaitValues.isEmpty()) {
            throw new IllegalArgumentException("Await list shouldn't be empty");
        }

        this.awaitValues = awaitValues;
        resetAwaitTimeValues();
    }

    public void resetAwaitTimeValues() {
        pos = -1;
    }

    public Long getTimeAwaitValue() {
        if (pos == -1) {
            return 0L;
        } else {
            return awaitValues.get(pos);
        }
    }

    public void incrementInterval() {
        if (pos < awaitValues.size() - 1) {
            pos++;
        }
    }
}

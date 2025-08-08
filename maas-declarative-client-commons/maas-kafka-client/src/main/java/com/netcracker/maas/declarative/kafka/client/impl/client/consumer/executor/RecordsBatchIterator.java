package org.qubership.maas.declarative.kafka.client.impl.client.consumer.executor;

import org.qubership.cloud.maas.bluegreen.kafka.CommitMarker;
import org.qubership.cloud.maas.bluegreen.kafka.Record;
import org.qubership.cloud.maas.bluegreen.kafka.RecordsBatch;
import org.apache.kafka.clients.consumer.ConsumerRecord;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

class RecordsBatchIterator<K, V> {
    private RecordsBatch<K, V> batch = null;
    private int pos = 0;
    private boolean processed = true;

    public void set(RecordsBatch<K, V> batch) {
        Objects.requireNonNull(batch);
        this.processed = false;
        this.batch = batch;
        this.pos = 0;
    }

    public boolean isProcessed() {
        return batch == null || processed;
    }

    public void markProcessed() {
        processed = true;
    }

    public List<ConsumerRecord<K, V>> handledRecords() {
        if (isProcessed()) {
            return Collections.emptyList();
        } else {
            return batch.getBatch()
                    .subList(0, pos)
                    .stream()
                    .map(r -> r.getConsumerRecord())
                    .toList();
        }
    }

    public void moveToNextRecord() {
        if (batch == null) {
            return;
        }

        pos++;
    }

    public Record<K, V> record() {
        if (batch != null && pos < batch.getBatch().size()) {
            return batch.getBatch().get(pos);
        } else {
            return null;
        }
    }

    public CommitMarker getBatchCommitMarker() {
        return batch.getCommitMarker();
    }

    public void reset() {
        this.batch = null;
        this.pos = 0;
        this.processed = true;
    }

    public String toString() {
        return batch == null ? "empty" :
                "batch size: " + batch.getBatch().size() + ", position: " + pos + ", commit: " + batch.getCommitMarker().getPosition();
    }
}

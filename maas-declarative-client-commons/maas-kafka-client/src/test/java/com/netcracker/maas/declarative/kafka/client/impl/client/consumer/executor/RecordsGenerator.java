package com.netcracker.maas.declarative.kafka.client.impl.client.consumer.executor;

import org.qubership.cloud.bluegreen.api.model.NamespaceVersion;
import org.qubership.cloud.bluegreen.api.model.State;
import org.qubership.cloud.bluegreen.api.model.Version;
import org.qubership.cloud.maas.bluegreen.kafka.CommitMarker;
import org.qubership.cloud.maas.bluegreen.kafka.Record;
import org.qubership.cloud.maas.bluegreen.kafka.RecordsBatch;
import lombok.SneakyThrows;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

class RecordsGenerator {
    AtomicInteger pos = new AtomicInteger();
    AtomicInteger iterationNumber = new AtomicInteger();

    @SneakyThrows
    public Optional<RecordsBatch> next() {
        iterationNumber.incrementAndGet();
        var count = 2;
        List<Record> records = IntStream.range(pos.get(), pos.addAndGet(count))
                .boxed()
                .map(i -> new Record(
                        new ConsumerRecord("orders", 0, i, "order" + (i+1), "data" + (i+1)),
                        createCommitMarker(i + 1))
                )
                .toList();
        return Optional.of(
                new RecordsBatch(
                        records,
                        createCommitMarker(pos.get())
                )
            );
    }

    /**
     * all records was filtered out by version filter
     * @return result
     */
    public Optional<RecordsBatch> nextEmpty() {
        iterationNumber.incrementAndGet();
        var count = 2;
        pos.addAndGet(count);
        return Optional.of(
                new RecordsBatch(
                        Collections.emptyList(),
                        createCommitMarker(pos.get())
                )
        );
    }

    private CommitMarker createCommitMarker(int pos) {
        return new CommitMarker(
                new NamespaceVersion(
                        "order-processor",
                        State.ACTIVE,
                        new Version("v1")
                ),
                Map.of(
                        new TopicPartition("orders", 0),
                        new OffsetAndMetadata(pos)
                )
        );
    }

    public int getIterationNumber() {
        return iterationNumber.get();
    }
}

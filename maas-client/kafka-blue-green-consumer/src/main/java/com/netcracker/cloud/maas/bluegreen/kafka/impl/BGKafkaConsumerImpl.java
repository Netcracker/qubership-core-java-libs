package com.netcracker.cloud.maas.bluegreen.kafka.impl;

import org.qubership.cloud.bluegreen.api.model.BlueGreenState;
import org.qubership.cloud.bluegreen.api.model.NamespaceVersion;
import org.qubership.cloud.bluegreen.api.model.State;
import org.qubership.cloud.bluegreen.api.service.BlueGreenStatePublisher;
import org.qubership.cloud.maas.bluegreen.kafka.BGKafkaConsumer;
import org.qubership.cloud.maas.bluegreen.kafka.CommitMarker;
import org.qubership.cloud.maas.bluegreen.kafka.Record;
import org.qubership.cloud.maas.bluegreen.kafka.RecordsBatch;
import org.qubership.cloud.maas.bluegreen.versiontracker.impl.VersionFilterConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.TopicPartition;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.qubership.cloud.framework.contexts.xversion.XVersionContextObject.X_VERSION_SERIALIZATION_NAME;
import static org.apache.kafka.clients.consumer.ConsumerConfig.AUTO_OFFSET_RESET_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.GROUP_ID_CONFIG;

// Warning: it is not thread safe
@Slf4j
public class BGKafkaConsumerImpl<K, V> implements BGKafkaConsumer<K, V> {
    private final BGKafkaConsumerConfig config;
    private final BlueGreenStatePublisher blueGreenStatePublisher;

    private Consumer<K, V> kafkaConsumer;
    private Predicate<String> filter = v -> true;

    private BlueGreenState activeState;
    private final AtomicReference<BlueGreenState> bgStateRef = new AtomicReference<>();

    public BGKafkaConsumerImpl(BGKafkaConsumerConfig config) {
        this.config = config;

        this.blueGreenStatePublisher = config.getStatePublisher();
        CompletableFuture<Void> awaitState = new CompletableFuture<>();
        blueGreenStatePublisher.subscribe(state -> {
            log.info("Received BG state from consul: {}", state);
            bgStateRef.set(state);
            awaitState.complete(null);
        });
        try {
            awaitState.get(30, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new RuntimeException("Consumer initialization error: timeout getting BG state");
        }
    }

    @Override
    public Optional<RecordsBatch<K, V>> poll(Duration timeout) {
        var bgState = bgStateRef.getAndSet(null);
        if (bgState != null) {
            if (bgState.getCurrent().getState() == State.IDLE) {
                // the namespace we are in received IDLE state, it means we are being cleaned up via commit operation
                log.warn("Current namespace is in IDLE state. Waiting for duration of specified timeout = {} and returning empty result.", timeout);
                try {
                    TimeUnit.SECONDS.sleep(timeout.toSeconds());
                } catch (InterruptedException ignored) {
                }
                return Optional.empty();
            }
            try {
                this.activeState = reinitializeConsumerIfNeeded(bgState);
            } catch (Exception e) {
                log.error("error initializing consumer for new state: {}", bgState);

                // we didn't finished initialization, so return bgState value to marker
                bgStateRef.compareAndSet(null, bgState);

                // inform user about exceptional state
                throw e;
            }
        }

        Objects.requireNonNull(activeState);

        ConsumerRecords<K, V> records = kafkaConsumer.poll(timeout);
        if (records.count() > 0) {
            List<Record<K, V>> result = new ArrayList<>(records.count());

            var position = new HashMap<TopicPartition, OffsetAndMetadata>();
            CommitMarker marker = null;
            for (ConsumerRecord<K, V> record : records) {
                String xVersion = extractVersionHeader(record);

                log.debug("Record's topic: {}, partition: {}, offset: {}, header: {}='{}'",
                        record.topic(), record.partition(), record.offset(), X_VERSION_SERIALIZATION_NAME, xVersion);

                position.put(new TopicPartition(record.topic(), record.partition()),
                        new OffsetAndMetadata(record.offset() + 1));

                // create marker with clone of position
                marker = new CommitMarker(activeState.getCurrent(), new HashMap<>(position));

                if (config.isIgnoreFilter() || filter.test(xVersion)) {
                    log.debug("Accept consumer record: {}", record);
                    result.add(new Record<>(record, marker));
                } else {
                    log.debug("Skip message due to unmatched header ({}='{}')", X_VERSION_SERIALIZATION_NAME, xVersion);
                }
            }
            return Optional.of(new RecordsBatch<>(result, marker));
        } else {
            return Optional.empty();
        }
    }

    @Override
    public void commitSync(CommitMarker marker) {
        if (activeState.getCurrent().equals(marker.getVersion())) {
            log.debug("Committing marker: {}", marker);
            kafkaConsumer.commitSync(marker.getPosition());
        } else {
            log.warn("Skip commits for non active consumer for: {}", marker.getVersion());
        }
    }

    @SneakyThrows
    public void close() {
        log.info("Closing consumer");
        if (blueGreenStatePublisher instanceof AutoCloseable closeable) {
            closeable.close();
        }
        Optional.ofNullable(kafkaConsumer).ifPresent(Consumer::close);
    }

    @Override
    public void pause() {
        Optional.ofNullable(kafkaConsumer).ifPresentOrElse(c -> c.pause(assignment()), () -> {
            throw new IllegalStateException("Consumer not initiated yet. Initiate with poll()");
        });
    }

    @Override
    public void resume() {
        Optional.ofNullable(kafkaConsumer).ifPresentOrElse(c -> c.resume(assignment()), () -> {
            throw new IllegalStateException("Consumer not initiated yet. Initiate with poll()");
        });
    }

    @Override
    public Set<TopicPartition> paused() {
        return Optional.ofNullable(kafkaConsumer).map(Consumer::paused)
                .orElseThrow(() -> new IllegalStateException("Consumer not initiated yet. Initiate with poll()"));
    }

    @Override
    public Collection<TopicPartition> assignment() {
        return Optional.ofNullable(kafkaConsumer).map(Consumer::assignment)
                .orElseThrow(() -> new IllegalStateException("Consumer not initiated yet. Initiate with poll()"));
    }

    private BlueGreenState reinitializeConsumerIfNeeded(BlueGreenState bgState) {
        log.info("Initialize consumer for {}", bgState);
        if (kafkaConsumer != null) {
            try {
                kafkaConsumer.close();
            } catch (Exception e) {
                // ignore this exception, because one crashed consumer doesn't affect to new one
                log.error("Error closing consumer", e);
            }
        }

        var current = bgState.getCurrent();
        var siblingNsState = bgState.getSibling().map(NamespaceVersion::getState);
        GroupId groupId;
        if (siblingNsState.isEmpty()) {
            groupId = new PlainGroupId(config.getGroupIdPrefix());
        } else {
            groupId = new VersionedGroupId(config.getGroupIdPrefix(), current.getVersion(), current.getState(), siblingNsState.get(), bgState.getUpdateTime());
        }

        var props = new HashMap<>(config.getProperties());
        // override group id to versioned one
        props.put(GROUP_ID_CONFIG, groupId.toString());
        if (props.get(AUTO_OFFSET_RESET_CONFIG) != null) {
            log.warn("`{}' is not suitable with BGKafkaConsumer usage. Please switch to use OffsetSetupStrategy enum. Ignored.", AUTO_OFFSET_RESET_CONFIG);
            props.remove(AUTO_OFFSET_RESET_CONFIG);
        }

        log.debug("Construct new instance of kafka consumer for groupId: '{}'", groupId);
        kafkaConsumer = config.getConsumerSupplier().apply(props);

        // use rebalance listener to perform offsets alignment only when our consumer was rebalanced by kafka
        ConsumerRebalanceListener rebalanceListener = new ConsumerRebalanceListener() {
            @Override
            public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
                log.debug("Partitions were revoked: {}", partitions);
            }

            @Override
            public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
                log.debug("Partitions were assigned: {}", partitions);
                log.debug("Start offset alignment process for: {}", groupId);
                OffsetsManager offsetsManager = new OffsetsManager(config, kafkaConsumer);
                Map<TopicPartition, OffsetAndMetadata> alignedOffsets = offsetsManager.alignOffset(groupId)
                        .entrySet().stream()
                        .filter(entry -> partitions.contains(entry.getKey()))
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
                log.debug("Adjusting current consumer offsets to: {}", alignedOffsets);
                alignedOffsets.forEach(((topicPartition, offsetAndMetadata) -> kafkaConsumer.seek(topicPartition, offsetAndMetadata)));
            }
        };
        log.debug("Subscribing kafka consumer to the topics: {}", config.getTopics().stream().sorted().toList());
        kafkaConsumer.subscribe(config.getTopics(), rebalanceListener);

        // update filter according to BG state
        filter = VersionFilterConstructor.constructVersionFilter(bgState);
        log.info("Version filter updated to: {}", filter);
        return bgState;
    }

    public static String extractVersionHeader(ConsumerRecord record) {
        return Arrays.stream(record.headers().toArray())
                .filter(h -> h.key().equalsIgnoreCase(X_VERSION_SERIALIZATION_NAME))
                .findFirst()
                .map(h -> new String(h.value(), StandardCharsets.UTF_8))
                .orElse("");
    }
}

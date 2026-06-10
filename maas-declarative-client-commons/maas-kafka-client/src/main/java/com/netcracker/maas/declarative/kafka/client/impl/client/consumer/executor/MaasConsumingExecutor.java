package com.netcracker.maas.declarative.kafka.client.impl.client.consumer.executor;

import com.netcracker.cloud.bluegreen.api.model.NamespaceVersion;
import com.netcracker.cloud.bluegreen.api.service.BlueGreenStatePublisher;
import com.netcracker.cloud.maas.bluegreen.kafka.BGKafkaConsumer;
import com.netcracker.cloud.maas.bluegreen.kafka.CommitMarker;
import com.netcracker.cloud.maas.bluegreen.kafka.Record;
import com.netcracker.maas.declarative.kafka.client.api.MaasKafkaConsumerErrorHandler;
import com.netcracker.maas.declarative.kafka.client.api.exception.MaasKafkaIllegalStateException;
import com.netcracker.maas.declarative.kafka.client.api.filter.ConsumerRecordFilter;
import com.netcracker.maas.declarative.kafka.client.api.filter.RecordFilter;
import com.netcracker.maas.declarative.kafka.client.impl.client.consumer.filter.Chain;
import com.netcracker.maas.declarative.kafka.client.impl.client.consumer.filter.impl.FilterExecutor;
import com.netcracker.maas.declarative.kafka.client.impl.client.creator.KafkaClientCreationService;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static com.netcracker.maas.declarative.kafka.client.impl.Utils.safe;

public class MaasConsumingExecutor implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(MaasConsumingExecutor.class);
    private static final int MIN_EFFECTIVE_BATCH = 3; // Floor for backpressure math when max.poll.records is very small (avoids pause/resume flicker).

    private final ConsumerInstanceHolder consumer;
    private final ConsumerExecContext context;
    private final AtomicReference<ExecutorState> stateRef = new AtomicReference<>(ExecutorState.INACTIVE);
    private final MaasKafkaConsumerErrorHandler errorHandler;
    private final AwaitExecutorService rescheduleTimeoutValueProvider;
    private final BlueGreenStatePublisher statePublisher;

    private final List<ConsumerRecordFilter> consumerRecordProcessors;
    private final ConsumerRecordFilter terminalRecordHandler = new ConsumerRecordFilter() {
        @Override
        public void doFilter(Record<?, ?> record, Chain<Record<?, ?>> next) {
            context.getHandler().accept(record.getConsumerRecord());
        }

        @Override
        public int order() {
            return Integer.MAX_VALUE;
        }
    };

    // backpressure thresholds derived from max.poll.records
    private final int queueCapacity;
    private final int pauseThreshold;
    private final int resumeThreshold;

    // shared state between executor thread and worker thread
    private final BlockingQueue<Record<?, ?>> workerQueue;
    // keyed by (partition, version) — NOT partition alone. The async queue can hold records polled before
    // and after a blue-green switch, so the same partition may be in flight under two generations at once
    // Offsets of different generations live in different consumer groups and are not comparable, so each
    // (partition, version) gets its own slot
    private final ConcurrentHashMap<CommitKey, CommitMarker> readyToCommit = new ConcurrentHashMap<>();
    private final AtomicBoolean shouldBePaused = new AtomicBoolean(false);
    private final AtomicReference<Exception> workerError = new AtomicReference<>();
    // set by the worker on a fatal error; stops it from processing further records so the committed
    // offset can never advance past a failed record. Reset by the consumer thread during recovery.
    private final AtomicBoolean processingHalted = new AtomicBoolean(false);
    private final AtomicBoolean running = new AtomicBoolean(false);
    private Thread workerThread;

    public MaasConsumingExecutor(ConsumerExecContext context,
                                 MaasKafkaConsumerErrorHandler errorHandler,
                                 KafkaClientCreationService kafkaClientCreationService,
                                 List<ConsumerRecordFilter> consumerRecordFilters,
                                 BlueGreenStatePublisher statePublisher) {
        this.context = context;
        this.errorHandler = errorHandler;
        this.rescheduleTimeoutValueProvider = new AwaitExecutorService(context.getAwaitAfterErrorTimeList());
        this.consumer = new ConsumerInstanceHolder(kafkaClientCreationService);
        this.statePublisher = statePublisher;

        this.consumerRecordProcessors = new ArrayList<>();
        this.consumerRecordProcessors.addAll(consumerRecordFilters);
        this.consumerRecordProcessors.sort(Comparator.comparingInt(RecordFilter::order));
        this.consumerRecordProcessors.add(terminalRecordHandler); // must be the very last one

        int maxPollRecords = Optional.ofNullable(context.getBlueGreenConfiguration())
                .map(c -> c.getConfigs().get(ConsumerConfig.MAX_POLL_RECORDS_CONFIG))
                .map(v -> Integer.parseInt(v.toString()))
                .orElse(500);
        int effectiveBatch = Math.max(maxPollRecords, MIN_EFFECTIVE_BATCH);
        this.queueCapacity = effectiveBatch * 3;
        this.pauseThreshold = effectiveBatch;
        this.resumeThreshold = effectiveBatch / 2;
        this.workerQueue = new LinkedBlockingQueue<>(queueCapacity);
        LOG.info("max.poll.records={}, effectiveBatch={}, queue capacity={}, pause>={}, resume<={}",
                maxPollRecords, effectiveBatch, queueCapacity, pauseThreshold, resumeThreshold);
    }

    public void init() {
        LOG.debug("Initializing consumer executor for topic: {}", context.getTopic().getTopicName());
        running.set(true);
        workerThread = new Thread(this::workerLoop,
                "maas-kafka-worker-" + context.getTopic().getTopicName());
        workerThread.setDaemon(true);
        workerThread.start();
        context.getExecutorService().execute(this);
    }

    @Override
    public void run() {
        var state = stateRef.get();
        LOG.debug("Run in state: {}", state);
        switch (state) {
            case SUSPENDED, INACTIVE -> {
                consumer.release();
                LOG.debug("Reschedule run after: 100ms");
                context.getExecutorService().schedule(this, 100, TimeUnit.MILLISECONDS);
            }
            case CLOSED -> {
                consumer.release();
                // do not reschedule
                LOG.info("Exit consuming loop");
            }
            case ACTIVE -> {
                try {
                    consume(consumer.getOrCreateInstance());
                    rescheduleTimeoutValueProvider.resetAwaitTimeValues();
                } catch (Exception e) {
                    LOG.error("Error consume records. Close current consumer and reschedule consuming with increased timeout", e);
                    safe(() -> consumer.release()); // force consumer recreate
                    rescheduleTimeoutValueProvider.incrementInterval();
                } finally {
                    var timeout = rescheduleTimeoutValueProvider.getTimeAwaitValue();
                    LOG.debug("Reschedule run after: {}ms", timeout);
                    context.getExecutorService().schedule(this, timeout, TimeUnit.MILLISECONDS);
                }
            }
            default -> {
                var errorMsg = "Unknown state enum value: " + state;
                LOG.error(errorMsg);
                throw new IllegalArgumentException(errorMsg);
            }
        }
    }

    public void consume(BGKafkaConsumer<?, ?> consumer) throws Exception {
        // commit offsets the worker has finished. commitSync blocks only for the broker round-trip (~ms)
        // done before the workerError check so successfully processed records are committed even when a later record's error handler failed
        if (!readyToCommit.isEmpty()) {
            Map<CommitKey, CommitMarker> snapshot = Map.copyOf(readyToCommit);
            snapshot.forEach(readyToCommit::remove); // conditional remove: only removes if value still matches
            // commit one marker per NamespaceVersion: the async queue can hold records polled before and
            // after a blue-green switch, so a snapshot may mix versions. Each commitSync carries a single
            // version, which BGKafkaConsumer gates against the active state — committing offsets from one
            // generation under another would corrupt the committed position.
            // switch of the consumer is internal to BGKafkaConsumerImpl (this.activeState = reinitializeConsumerIfNeeded)
            for (CommitMarker marker : groupByVersion(snapshot)) {
                LOG.debug("[consumer] committing version={} position={}", marker.getVersion(), marker.getPosition());
                consumer.commitSync(marker);
            }
        }

        // rethrow any unrecoverable worker error — triggers consumer.release() in run()
        // drop in-flight records: the recreated consumer re-fetches from the last committed offset
        Exception err = workerError.getAndSet(null);
        if (err != null) {
            readyToCommit.clear();
            workerQueue.clear();
            // worker halted on the fatal error and stopped advancing the committed offset
            // clear the halt so it can resume once the recreated consumer re-fetches from the last committed offset.
            processingHalted.set(false);
            throw err;
        }

        // backpressure: update desired pause state based on queue fill
        int queueSize = workerQueue.size();
        if (!shouldBePaused.get() && queueSize >= pauseThreshold) {
            shouldBePaused.set(true);
            LOG.debug("[consumer] PAUSED — queue size {} >= threshold {}", queueSize, pauseThreshold);
        } else if (shouldBePaused.get() && queueSize <= resumeThreshold) {
            shouldBePaused.set(false);
            LOG.debug("[consumer] RESUMED — queue size {} <= threshold {}", queueSize, resumeThreshold);
        }

        // apply pause/resume — single place where resume is called (pause is called in emergency and in listener)
        if (shouldBePaused.get()) {
            consumer.pause();
        } else {
            consumer.resume();
        }

        // poll — short timeout when paused (heartbeat only, no fetches)
        Duration pollTimeout = shouldBePaused.get() ? Duration.ofMillis(200) : context.getPollDuration(); 
        consumer.poll(pollTimeout).ifPresent(batch -> {
            for (Record<?, ?> record : batch.getBatch()) {
                if (!workerQueue.offer(record)) {
                    // emergency: queue hit capacity. it could happen if poll returned more records than max.poll.records
                    // Pause BEFORE spinning so poll(0) can only send
                    // heartbeats and can never fetch-and-drop records
                    shouldBePaused.set(true);
                    consumer.pause();
                    do {
                        // NOT wrapped in safe() — exception must propagate to trigger normal error recovery
                        LOG.debug("[consumer] queue full ({}/{}), paused; calling poll(0) to stay alive",
                                workerQueue.size(), queueCapacity);
                        try {
                            consumer.poll(Duration.ZERO); // send heartbeat only, emergency loop and need to offer again asap, only network delay
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    } while (!workerQueue.offer(record));
                }
                LOG.debug("[consumer] queued partition={} offset={} key={}",
                        record.getConsumerRecord().partition(), record.getConsumerRecord().offset(),
                        record.getConsumerRecord().key());
            }
        });
    }

    private void workerLoop() {
        LOG.info("[worker] started for topic: {}", context.getTopic().getTopicName());
        while (running.get()) {
            try {
                if (processingHalted.get()) {
                    // fatal error pending — do NOT touch the queue until the consumer thread recovers,
                    // otherwise later successes would push the committed offset past the failed record
                    Thread.sleep(50);
                    continue;
                }
                Record<?, ?> record = workerQueue.poll(200, TimeUnit.MILLISECONDS);
                if (record == null) continue;

                LOG.debug("[worker] processing partition={} offset={} key={}",
                        record.getConsumerRecord().partition(), record.getConsumerRecord().offset(),
                        record.getConsumerRecord().key());
                try {
                    FilterExecutor.execute(consumerRecordProcessors, record);
                    // mark offset as ready to commit. Key is (partition, version) so blue-green generations
                    // never overwrite each other. Compare this partition's offset only — BG markers are
                    // cumulative across partitions; a global max would let another partition mask a real advance.
                    var partition = new TopicPartition(record.getConsumerRecord().topic(), record.getConsumerRecord().partition());
                    readyToCommit.merge(
                            new CommitKey(partition, record.getCommitMarker().getVersion()),
                            record.getCommitMarker(),
                            (existing, latest) -> offsetOf(latest, partition) > offsetOf(existing, partition) ? latest : existing
                    );
                    LOG.debug("[worker] done partition={} offset={}",
                            record.getConsumerRecord().partition(), record.getConsumerRecord().offset());
                } catch (Exception processingException) {
                    try {
                        LOG.warn("[worker] handle record processing exception: {}", processingException.getMessage());
                        errorHandler.handle(processingException, record.getConsumerRecord(), List.of());  //List.of empty because we don't have any records to send -- they are now committed in consumer loop separately
                    } catch (Exception errorHandlerException) {
                        LOG.error("[worker] error handler threw — halting worker until recovery", errorHandlerException);
                        errorHandlerException.addSuppressed(processingException);
                        // halt BEFORE publishing the error so the next loop iteration cannot dequeue and
                        // process another record (which would advance the committed offset past this one)
                        processingHalted.set(true);
                        workerError.set(errorHandlerException);
                        // do not exit loop — consumer thread will trigger recovery on next run()
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        LOG.info("[worker] stopped for topic: {}", context.getTopic().getTopicName());
    }

    private static long offsetOf(CommitMarker marker, TopicPartition partition) {
        OffsetAndMetadata offset = marker.getPosition().get(partition);
        return offset != null ? offset.offset() : -1L;
    }

    private static Collection<CommitMarker> groupByVersion(Map<CommitKey, CommitMarker> snapshot) {
        // poll returns one generation, but the queue can hold records from previous generations.
        // group offsets by their owning NamespaceVersion so each is committed under its own generation.
        // NamespaceVersion is used as a map key, so a null version (DefaultKafkaConsumer) needs a wrapper.
        Map<Optional<NamespaceVersion>, Map<TopicPartition, OffsetAndMetadata>> byVersion = new HashMap<>();
        snapshot.forEach((key, marker) -> {
            Map<TopicPartition, OffsetAndMetadata> offsets =
                    byVersion.computeIfAbsent(Optional.ofNullable(key.version()), v -> new HashMap<>());
            // each marker is cumulative across partitions; multiple (partition, version) keys for the same
            // generation can carry overlapping entries — per-partition max, never blind putAll.
            marker.getPosition().forEach((tp, off) ->
                    offsets.merge(tp, off, (a, b) -> b.offset() > a.offset() ? b : a));
        });

        List<CommitMarker> result = new ArrayList<>(byVersion.size());
        byVersion.forEach((version, offsets) -> result.add(new CommitMarker(version.orElse(null), offsets)));
        return result;
    }

    // composite key so the same partition under two blue-green generations occupies distinct slots in
    // readyToCommit; version may be null for the non-blue-green DefaultKafkaConsumer (records handle null).
    private record CommitKey(TopicPartition partition, NamespaceVersion version) {
    }

    public void suspend() {
        LOG.info("Suspend executor");
        stateRef.set(ExecutorState.SUSPENDED);
    }

    public void resume() {
        LOG.info("Resume executor");
        stateRef.set(ExecutorState.ACTIVE);
    }

    public void close() {
        LOG.info("Close executor");
        stateRef.set(ExecutorState.CLOSED);
        running.set(false);
        if (workerThread != null) {
            workerThread.interrupt(); // if we are on workerQueue.poll()
            try {
                workerThread.join(5_000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public void start() {
        if (!stateRef.compareAndSet(ExecutorState.INACTIVE, ExecutorState.ACTIVE)) {
            throw new MaasKafkaIllegalStateException("Impossible start already started Executor");
        }
    }

    class ConsumerInstanceHolder {
        private final AtomicReference<BGKafkaConsumer<?, ?>> instance = new AtomicReference<>();
        private final KafkaClientCreationService kafkaClientCreationService;

        ConsumerInstanceHolder(KafkaClientCreationService kafkaClientCreationService) {
            this.kafkaClientCreationService = kafkaClientCreationService;
        }

        public BGKafkaConsumer<?, ?> getOrCreateInstance() {
            withSync(() -> instance.get() == null, () -> {
                LOG.info("Create new kafka consumer instance");
                BGKafkaConsumer<?, ?> newConsumer = kafkaClientCreationService.createKafkaConsumer(
                        context.getBlueGreenConfiguration(),
                        context.getDeserializerHolder().getKeyDeserializer(),
                        context.getDeserializerHolder().getValueDeserializer(),
                        context.getTopic().getTopicName(),
                        null,
                        statePublisher
                );

                // re-apply pause inside onPartitionsAssigned to close the rebalance window:
                // kafka clears pause on revoke, but fetch for the new assignment hasn't been sent yet —
                // pausing here prevents any records from slipping through in the same poll() call
                newConsumer.setPartitionsAssignedListener(partitions -> {
                    if (shouldBePaused.get()) {
                        newConsumer.pause();
                        LOG.debug("[consumer] re-applied pause after rebalance on partitions={}", partitions);
                    }
                });
                instance.set(newConsumer);
            });
            return instance.get();
        }

        public void release() {
            withSync(() -> instance.get() != null, () -> {
                try {
                    LOG.info("Close kafka consumer");
                    instance.get().close();
                } catch (Exception e) {
                    LOG.error("Error close consumer instance for topic: {}", context.getTopic().getTopicName(), e);
                } finally {
                    instance.set(null);
                }
            });
        }

        private void withSync(Supplier<Boolean> clause, Runnable updater) {
            if (clause.get()) {
                synchronized (instance) {
                    if (clause.get()) {
                        updater.run();
                    }
                }
            }
        }
    }
}

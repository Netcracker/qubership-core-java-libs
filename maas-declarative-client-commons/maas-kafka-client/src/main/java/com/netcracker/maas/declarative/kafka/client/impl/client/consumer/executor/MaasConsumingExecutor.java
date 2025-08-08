package org.qubership.maas.declarative.kafka.client.impl.client.consumer.executor;

import org.qubership.cloud.bluegreen.api.service.BlueGreenStatePublisher;
import org.qubership.cloud.maas.bluegreen.kafka.BGKafkaConsumer;
import org.qubership.cloud.maas.bluegreen.kafka.CommitMarker;
import org.qubership.cloud.maas.bluegreen.kafka.Record;
import org.qubership.maas.declarative.kafka.client.api.MaasKafkaConsumerErrorHandler;
import org.qubership.maas.declarative.kafka.client.api.exception.MaasKafkaIllegalStateException;
import org.qubership.maas.declarative.kafka.client.api.filter.ConsumerRecordFilter;
import org.qubership.maas.declarative.kafka.client.api.filter.RecordFilter;
import org.qubership.maas.declarative.kafka.client.impl.client.consumer.filter.Chain;
import org.qubership.maas.declarative.kafka.client.impl.client.consumer.filter.impl.FilterExecutor;
import org.qubership.maas.declarative.kafka.client.impl.client.creator.KafkaClientCreationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static org.qubership.maas.declarative.kafka.client.impl.Utils.safe;

public class MaasConsumingExecutor implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(MaasConsumingExecutor.class);

    private final ConsumerInstanceHolder consumer;
    private final ConsumerExecContext context;
    private final AtomicReference<ExecutorState> stateRef = new AtomicReference<>(ExecutorState.INACTIVE);
    private final MaasKafkaConsumerErrorHandler errorHandler;
    private final AwaitExecutorService rescheduleTimeoutValueProvider;
    private final RecordsBatchIterator batchIterator = new RecordsBatchIterator();
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
    }

    public void init() {
        LOG.debug("Initializing consumer executor for topic: {}", context.getTopic().getTopicName());
        context.getExecutorService().execute(this);
    }

    @Override
    public void run() {
        var state = stateRef.get();
        LOG.debug("Run in state: {}", state);
        switch(state) {
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
        if (batchIterator.isProcessed()) {
            LOG.debug("Poll records and refill iterator");
            consumer.poll(context.getPollDuration()).ifPresent(batchIterator::set);
        }

        if (!batchIterator.isProcessed()) {
            LOG.debug("Process batch: {}", batchIterator);
            CommitMarker lastProcessedRecordMarker = null;
            while(batchIterator.record() != null) {
                try {
                    LOG.debug("Process record: {}", batchIterator.record());
                    FilterExecutor.execute(consumerRecordProcessors, batchIterator.record());
                    lastProcessedRecordMarker = batchIterator.record().getCommitMarker();
                } catch (Exception processingException) {
                    try {
                        LOG.debug("Handle record processing exception: {}", processingException.getMessage());
                        errorHandler.handle(processingException,
                                batchIterator.record().getConsumerRecord(),
                                batchIterator.handledRecords());
                    } catch (Exception errorHandleException) {
                        LOG.debug("Exception in record error handler. Try to commit already processed records and rethrow: {}", errorHandleException.getMessage());
                        Optional.ofNullable(lastProcessedRecordMarker).ifPresent(m -> safe(() -> consumer.commitSync(m)));
                        batchIterator.reset();
                        errorHandleException.addSuppressed(processingException);
                        throw errorHandleException;
                    }
                }
                // move to next record either if record successfully processed or, in case of error, successfully error handled
                batchIterator.moveToNextRecord();
            }

            LOG.debug("Commit records batch offset: {}", batchIterator.getBatchCommitMarker());
            consumer.commitSync(batchIterator.getBatchCommitMarker());
            batchIterator.markProcessed();
        } else {
            LOG.debug("nothing to process");
        }
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
                instance.set(kafkaClientCreationService.createKafkaConsumer(
                        context.getBlueGreenConfiguration(),
                        context.getDeserializerHolder().getKeyDeserializer(),
                        context.getDeserializerHolder().getValueDeserializer(),
                        context.getTopic().getTopicName(),
                        null,
                        statePublisher
                ));
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

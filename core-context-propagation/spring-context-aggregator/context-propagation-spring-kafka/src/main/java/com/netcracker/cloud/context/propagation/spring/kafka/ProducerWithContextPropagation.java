package com.netcracker.cloud.context.propagation.spring.kafka;

import org.apache.kafka.clients.consumer.ConsumerGroupMetadata;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.*;
import org.apache.kafka.common.errors.ProducerFencedException;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.metrics.KafkaMetric;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

public class ProducerWithContextPropagation<K, V> implements Producer<K, V> {
	private static final Logger log = LoggerFactory.getLogger(ProducerWithContextPropagation.class);
	private Producer<K, V> delegate;

	public ProducerWithContextPropagation(String beanName, Producer delegate) {
		log.info("Init context propagation wrapper for: beanName={}, bean={}", beanName, delegate);
		this.delegate = delegate;
	}


	@Override
	public void initTransactions() {
		delegate.initTransactions();
	}

	@Override
	public void beginTransaction() throws ProducerFencedException {
		delegate.beginTransaction();
	}

	@Override
	public void sendOffsetsToTransaction(Map<TopicPartition, OffsetAndMetadata> offsets, ConsumerGroupMetadata groupMetadata) throws ProducerFencedException {
		delegate.sendOffsetsToTransaction(offsets, groupMetadata);
	}

	@Override
	public void commitTransaction() throws ProducerFencedException {
		delegate.commitTransaction();
	}

	@Override
	public void abortTransaction() throws ProducerFencedException {
		delegate.abortTransaction();
	}

    @Override
    public void registerMetricForSubscription(KafkaMetric kafkaMetric) {
        delegate.registerMetricForSubscription(kafkaMetric);
    }

    @Override
    public void unregisterMetricFromSubscription(KafkaMetric kafkaMetric) {
        delegate.unregisterMetricFromSubscription(kafkaMetric);
    }

    private void dumpContextIntoHeaders(ProducerRecord<K, V> record) {
		log.debug("Propagate context into headers for: {}", record);
		for (Header header : KafkaContextPropagation.propagateContext()) {
			record.headers().add(header);
		}
	}

	@Override
	public Future<RecordMetadata> send(ProducerRecord<K, V> record) {
		dumpContextIntoHeaders(record);
		return delegate.send(record);
	}

	@Override
	public Future<RecordMetadata> send(ProducerRecord<K, V> record, Callback callback) {
		dumpContextIntoHeaders(record);
		return delegate.send(record, callback);
	}

	@Override
	public void flush() {
		delegate.flush();
	}

	@Override
	public List<PartitionInfo> partitionsFor(String topic) {
		return delegate.partitionsFor(topic);
	}

	@Override
	public Map<MetricName, ? extends Metric> metrics() {
		return delegate.metrics();
	}

	@Override
	public Uuid clientInstanceId(Duration duration) {
		return delegate.clientInstanceId(duration);
	}

	@Override
	public void close() {
		delegate.close();
	}

	@Override
	public void close(Duration timeout) {
		delegate.close(timeout);
	}
}

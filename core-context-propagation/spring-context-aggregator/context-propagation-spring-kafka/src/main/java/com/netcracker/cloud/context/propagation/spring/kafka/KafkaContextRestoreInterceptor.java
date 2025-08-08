package com.netcracker.cloud.context.propagation.spring.kafka;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.listener.RecordInterceptor;

public class KafkaContextRestoreInterceptor implements RecordInterceptor<Object, Object> {
	private static final Logger log = LoggerFactory.getLogger(KafkaContextRestoreInterceptor.class);
	private final RecordInterceptor chained;

	public KafkaContextRestoreInterceptor(RecordInterceptor chained) {
		this.chained = chained;
	}

	@Override
	public ConsumerRecord<Object, Object> intercept(ConsumerRecord<Object, Object> record, Consumer<Object, Object> consumer) {
		log.debug("Restore context from: {}", record);
		KafkaContextPropagation.restoreContext(record.headers());

		if (chained != null) {
			log.trace("Call original interceptor: {}", chained);
			record = chained.intercept(record, consumer);
		}

		return record;
	}
}

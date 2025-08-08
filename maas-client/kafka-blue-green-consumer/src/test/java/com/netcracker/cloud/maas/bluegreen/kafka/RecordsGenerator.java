package com.netcracker.cloud.maas.bluegreen.kafka;

import org.qubership.cloud.framework.contexts.xversion.XVersionContextObject;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.apache.kafka.common.record.TimestampType;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;

import static java.lang.Math.min;

class RecordsGenerator {
	private final static int PARTITION = 1;

	private int position;
	private final String topic;
	private final int total;
	private final Function<Integer, String> versionSupplier;

	public RecordsGenerator(String topic, int total, Function<Integer, String> versionSupplier) {
		this.topic = topic;
		this.total = total;
		this.versionSupplier = versionSupplier;
	}

	public ConsumerRecords<Long, String> next(int chunkSize) {
		List<ConsumerRecord<Long, String>> records = new ArrayList<>();
		int stop = min(position + chunkSize, total);
		for(int offset=position; offset<stop; offset++) {
			String message = "message: " + offset;

			Headers headers = new RecordHeaders();
			headers.add(
					new RecordHeader(
							XVersionContextObject.X_VERSION_SERIALIZATION_NAME,
							versionSupplier.apply(offset).getBytes(StandardCharsets.UTF_8)
					)
			);

			records.add(new ConsumerRecord<>(
					topic,
					PARTITION,
					offset,
					System.currentTimeMillis(),
					TimestampType.CREATE_TIME,
					Long.BYTES,
					message.getBytes(StandardCharsets.UTF_8).length,
					Long.valueOf(offset),
					message,
					headers,
					Optional.empty()));
		}
		position = stop;

		TopicPartition tp = new TopicPartition(topic, 1);
		Map<TopicPartition, List<ConsumerRecord<Long, String>>> dist = new HashMap<>();
		dist.put(tp, records);
		return new ConsumerRecords<>(dist);
	}

	boolean isEmpty() {
		return position == total;
	}
}

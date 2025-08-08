package com.netcracker.cloud.maas.client.api.kafka.protocolextractors;

import org.qubership.cloud.maas.client.api.kafka.TopicAddress;
import lombok.NonNull;

import java.util.Map;
import java.util.Optional;

public interface ConnectionPropertiesExtractor extends Comparable {
	/**
	 * Extracts connection properties list capable to be used for kafka producer/consumer instantiation
	 * @param addr {@link TopicAddress} as source of connection parameters
	 * @return List of connection properties wrapped into Optional, or Optional.empty() if extractor can't construct connections props by given topic info
	 */
	Optional<Map<String, Object>> extract(TopicAddress addr);

	/**
	 * Extractor priority number for ordering extractor execution priority
	 * @return priority number, bigger number has a highest priority
	 */
	int getExtractorPriority();

	@Override
	default int compareTo(@NonNull Object o) {
		return ((ConnectionPropertiesExtractor)o).getExtractorPriority() - getExtractorPriority();
	}
}

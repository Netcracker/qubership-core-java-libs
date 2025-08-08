package com.netcracker.cloud.maas.client.api.kafka;

import org.qubership.cloud.maas.client.api.Classifier;

import java.util.Map;
import java.util.Optional;

/**
 * Represents address of topic in Kafka
 */
public interface TopicAddress {
	/**
	 * Get classifier assigned to this topic item
	 * @return structure representing classifier fields
	 */
	Classifier getClassifier();

	/**
	 * Returns real topic name in Kafka
	 * @return string - real topic name in Kafka
	 */
    String getTopicName();

    /**
	 * Get formatted string of broker addresses for given `protocol' suitable to use as value for `bootstrap.servers' property
	 * in KafkaProducer/KafkaConsumer properties list
	 * @param protocol security protocol
	 * @return formatted address list separated by comma or null if no info was found for given protocol
	 */
	String getBoostrapServers(String protocol);

	/**
	 * Get topic client credentials by type
	 * @param type credential type  string
	 * @return credentials container
	 */
	Optional<TopicUserCredentials> getCredentials(String type);

	/**
	 * Get CA certificate. Value exactly the same as it was entered by MaaS managers during instance registration.
	 * Usually values is CA cert as base64 encoded string
	 * @return CA certificate as string
	 */
	String getCACert();

	/**
	 * Returns actual number of partitions for this topic in Kafka
	 * @return actual count of partitions on kafka
	 */
	int getNumPartitions();

	/**
	 * Create connection properties list from topic info. These properties can be used to create KafkaConsumer or KafkaProducer instances.
	 *
	 * @return  Optional with map of connection parameters or Optional.empty() in case topic connection options can't be recognized as
	 * any known supported security protocol.
	 */
	Optional<Map<String, Object>> formatConnectionProperties();

	/**
	 * Returns versioned status
	 * @return versioned status
	 */
	boolean isVersioned();

	/**
	 * Topic config property list
	 * @return map of real topic configuration parameters from Kafka broker
	 */
	default Map<String, String> getConfigs() {
		throw new UnsupportedOperationException("Not yet implemented");
	}
}

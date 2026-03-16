package com.netcracker.cloud.maas.client.api.kafka;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.netcracker.cloud.maas.client.api.kafka.protocolextractors.OnTopicExists;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.List;
import java.util.Map;

@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@Jacksonized
public class TopicCreateOptions {
	public final static TopicCreateOptions DEFAULTS = TopicCreateOptions.builder().build();

	/**
	 * What to do if requested topic already presented in Kafka broker
	 */
	@JsonIgnore
	@Builder.Default
	OnTopicExists onTopicExists = OnTopicExists.FAIL;

	/**
	 * This property allows to override maas default topic naming template like `maas.{namespace}.{name}'. You can specify exact name of topic that
	 * should be created in kafka.
	 */
	String name;

	/**
	 * Required exact number of topic partitions for topic. Mutual exclusive to {@code minNumPartitions}
	 */
	int numPartitions;

	/**
	 * Required minimum number of topic partitions. Mutual exclusive to {@code numPartitions}
	 */
	int minNumPartitions;

	/**
	 * Replication factor number
	 */
	String replicationFactor;

	/**
	 * Replica assignments
	 */
	@Singular("replicaAssignment")
	Map<String, List<Integer>> replicaAssignment;

	/**
	 * this topic is already exists and managed by somebody else.
	 * MaaS shouldn't do any create/update/delete operations on this topic is property set to true
	 */
	boolean externallyManaged;

	/**
	 * Template name for topic
	 */
	String template;

	/**
	 * Is topic versioned and should be copied on BG warmup
	 */
	boolean versioned;

	/**
	 *  Config properties for this topic
	 */
	@Singular("config")
	Map<String, String> configs;

	// Override some of builder methods provided by lombok
	public static class TopicCreateOptionsBuilder {
		public TopicCreateOptionsBuilder replicationFactor(int factor) {
			this.replicationFactor = String.valueOf(factor);
			return this;
		}

		public TopicCreateOptionsBuilder inheritReplicationFactor() {
			this.replicationFactor = "inherit";
			return this;
		}

		public TopicCreateOptionsBuilder numPartitions(int count) {
			if (minNumPartitions != 0) {
				throw new IllegalStateException("numPartitions is mutual exclusive to minNumPartitions. Choose only one property to set.");
			}

			this.numPartitions = count;
			return this;
		}

		public TopicCreateOptionsBuilder minNumPartitions(int count) {
			if (numPartitions != 0) {
				throw new IllegalStateException("minNumPartitions is mutual exclusive to `numPartitions'. Choose which one property is to set.");
			}

			this.minNumPartitions = count;
			return this;
		}
	}
}

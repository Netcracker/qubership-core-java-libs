package org.qubership.cloud.maas.client.api.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class TopicCreateOptionsTest {
	@Test
	void testBuilderEmpty() {
		var options = TopicCreateOptions.builder().build();
		assertEquals("{}", serialize(options));
	}

	@Test
	void testBuilder() throws JsonProcessingException {
		var options = TopicCreateOptions.builder()
				.name("abc")
				.numPartitions(2)
				.replicationFactor(10)
				.replicaAssignment("0", List.of(1,2,3))
				.config("timeout", "10")
				.template("base")
				.build();

		assertEquals("abc", options.getName());
		assertEquals(2, options.getNumPartitions());
		assertEquals("10", options.getReplicationFactor());
		assertEquals(Map.of("0", List.of(1,2,3)), options.getReplicaAssignment());
		assertEquals(Map.of("timeout", "10"), options.getConfigs());
		assertEquals("base", options.getTemplate());
	}

	@Test
	void testBuilder2() throws JsonProcessingException {
		var options = TopicCreateOptions.builder()
				.inheritReplicationFactor()
				.build();

		assertEquals("inherit", options.getReplicationFactor());
	}

	@Test
	void testNumPartitionsMutualExclusive() {
		assertThrows(IllegalStateException.class, () ->
				TopicCreateOptions.builder()
				.numPartitions(2)
				.minNumPartitions(3)
		);
	}

	@SneakyThrows
	String serialize(TopicCreateOptions options) {
		ObjectMapper objectMapper = new ObjectMapper();
		return objectMapper.writeValueAsString(options);
	}
}

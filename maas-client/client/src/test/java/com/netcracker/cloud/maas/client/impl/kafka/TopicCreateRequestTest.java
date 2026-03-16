package com.netcracker.cloud.maas.client.impl.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netcracker.cloud.maas.client.api.Classifier;
import com.netcracker.cloud.maas.client.api.kafka.TopicCreateOptions;
import com.netcracker.cloud.maas.client.impl.Env;
import com.netcracker.cloud.maas.client.impl.dto.kafka.v1.TopicRequest;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

import static com.netcracker.cloud.maas.client.Utils.withProp;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TopicCreateRequestTest {
	@SneakyThrows
	@Test
	void testSerialization() {
		withProp(Env.PROP_NAMESPACE, "core-dev", () -> {
			var req = TopicRequest.builder(new Classifier("foo")).build()
					.options(TopicCreateOptions.builder()
							.name("real-topic-name")
							.replicationFactor(10)
							.minNumPartitions(12)
							.config("flash.ms", "1200")
							.build());

			var mapper = new ObjectMapper();
			var json = "{\"classifier\":{\"name\":\"foo\",\"namespace\":\"core-dev\"},\"name\":\"real-topic-name\",\"minNumPartitions\":12,\"replicationFactor\":\"10\",\"configs\":{\"flash.ms\":\"1200\"}}";
			var actual = mapper.writeValueAsString(req);

			// serialization
			assertEquals(mapper.readTree(json), mapper.readTree(actual));

			// deserialization
			assertEquals(req, mapper.readValue(json, TopicRequest.class));
		});
	}
}

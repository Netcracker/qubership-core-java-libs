package org.qubership.cloud.maas.client.impl.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.qubership.cloud.maas.client.Utils;
import org.qubership.cloud.maas.client.api.kafka.TopicAddress;
import org.qubership.cloud.maas.client.impl.dto.kafka.v1.TopicInfo;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TopicAddressImplTest {
	ObjectMapper mapper = new ObjectMapper();

	@Test
	public void testEmptyCredentials() throws JsonProcessingException {
		String json = "{\n" +
				"    \"addresses\": {\n" +
				"        \"PLAINTEXT\": [\n" +
				"            \"localkafka.default:9092\"\n" +
				"        ]\n" +
				"    },\n" +
				"    \"name\": \"local.sampleTopic2.430d243ba9c74f25bf05f492a52804fd\",\n" +
				"    \"classifier\": {\n" +
				"        \"name\": \"sampleTopic2\",\n" +
				"        \"namespace\": \"local\"\n" +
				"    },\n" +
				"    \"namespace\": \"local\",\n" +
				"    \"instance\": \"localkafka\",\n" +
				"    \"caCert\": \"\",\n" +
				"    \"credential\": null,\n" +
				"    \"requestedSettings\": {\n" +
				"        \"numPartitions\": 1,\n" +
				"        \"replicationFactor\": 1,\n" +
				"        \"replicaAssignment\": null,\n" +
				"        \"configs\": null\n" +
				"    },\n" +
				"    \"actualSettings\": {\n" +
				"        \"numPartitions\": 1,\n" +
				"        \"replicationFactor\": 1,\n" +
				"        \"replicaAssignment\": {\n" +
				"            \"0\": [\n" +
				"                0\n" +
				"            ]\n" +
				"        },\n" +
				"        \"configs\": {\n" +
				"            \"cleanup.policy\": \"delete\",\n" +
				"            \"unclean.leader.election.enable\": \"false\"\n" +
				"        }\n" +
				"    }\n" +
				"}\n";
		TopicInfo topicInfo = mapper.readValue(json, TopicInfo.class);
		TopicAddress topicAddress = new TopicAddressImpl(topicInfo);
		assertNull(topicAddress.getBoostrapServers("missing"));
		assertFalse(topicAddress.getCredentials("SCRAM").isPresent());
		assertEquals(1, topicAddress.getNumPartitions());
		assertEquals("delete", topicAddress.getConfigs().get("cleanup.policy"));
	}

	@Test
	public void testNortmalClientCredentials() throws JsonProcessingException {
		String json = "{\n" +
				"        \"addresses\": {\n" +
				"            \"SASL_PLAINTEXT\": [\n" +
				"                \"kafka.cpq-kafka:9092\"\n" +
				"            ]\n" +
				"        },\n" +
				"        \"name\": \"cloudbss311-platform-core-support-dev3.sampleTopic2.0981cc8d4e904d93a3aa3d47db12c1c1\",\n" +
				"        \"classifier\": {\n" +
				"            \"name\": \"sampleTopic2\",\n" +
				"            \"namespace\": \"cloudbss311-platform-core-support-dev3\"\n" +
				"        },\n" +
				"        \"namespace\": \"cloudbss311-platform-core-support-dev3\",\n" +
				"        \"instance\": \"cpq-kafka\",\n" +
				"        \"caCert\": \"\",\n" +
				"        \"credential\": {\n" +
				"        },\n" +
				"        \"requestedSettings\": {\n" +
				"            \"numPartitions\": 1,\n" +
				"            \"replicationFactor\": 1,\n" +
				"            \"replicaAssignment\": null,\n" +
				"            \"configs\": null\n" +
				"        },\n" +
				"        \"actualSettings\": {\n" +
				"            \"numPartitions\": 1,\n" +
				"            \"replicationFactor\": 1,\n" +
				"            \"replicaAssignment\": {\n" +
				"                \"0\": [ 0 ]\n" +
				"            },\n" +
				"            \"configs\": {\n" +
				"                \"cleanup.policy\": \"delete\"\n" +
				"            }\n" +
				"        }\n" +
				"    }";
		TopicInfo topicInfo = mapper.readValue(json, TopicInfo.class);
		TopicAddressImpl topicAddress = new TopicAddressImpl(topicInfo);
		assertFalse(topicAddress.getCredentials("scram").isPresent());
	}

	@Test
	public void testMissingClientCredentials() throws JsonProcessingException {
		String json = "{\n" +
				"        \"addresses\": {\n" +
				"            \"SASL_PLAINTEXT\": [\n" +
				"                \"kafka.cpq-kafka:9092\"\n" +
				"            ]\n" +
				"        },\n" +
				"        \"name\": \"cloudbss311-platform-core-support-dev3.sampleTopic2.0981cc8d4e904d93a3aa3d47db12c1c1\",\n" +
				"        \"classifier\": {\n" +
				"            \"name\": \"sampleTopic2\",\n" +
				"            \"namespace\": \"cloudbss311-platform-core-support-dev3\"\n" +
				"        },\n" +
				"        \"namespace\": \"cloudbss311-platform-core-support-dev3\",\n" +
				"        \"instance\": \"cpq-kafka\",\n" +
				"        \"caCert\": \"\",\n" +
				"        \"credential\": {\n" +
				"            \"client\": [\n" +
				"                {\n" +
				"                    \"password\": \"plain:secret\",\n" +
				"                    \"type\": \"SCRAM\",\n" +
				"                    \"username\": \"client\"\n" +
				"                }\n" +
				"            ]\n" +
				"        },\n" +
				"        \"requestedSettings\": {\n" +
				"            \"numPartitions\": 0,\n" +
				"            \"minNumPartitions\": 1,\n" +
				"            \"replicationFactor\": 1,\n" +
				"            \"replicaAssignment\": null,\n" +
				"            \"configs\": null\n" +
				"        },\n" +
				"        \"actualSettings\": {\n" +
				"            \"numPartitions\": 1,\n" +
				"            \"replicationFactor\": 1,\n" +
				"            \"replicaAssignment\": {\n" +
				"                \"0\": [ 0 ]\n" +
				"            },\n" +
				"            \"configs\": {\n" +
				"                \"cleanup.policy\": \"delete\"\n" +
				"            }\n" +
				"        }\n" +
				"    }";
		TopicInfo topicInfo = mapper.readValue(json, TopicInfo.class);
		TopicAddressImpl topicAddress = new TopicAddressImpl(topicInfo);
		assertTrue(topicAddress.getCredentials("scram").isPresent());
		assertEquals("client", topicAddress.getCredentials("scram").get().getUsername());
		assertEquals("secret", topicAddress.getCredentials("scram").get().getPassword());
	}

	@Test
	public void testEquals() throws JsonProcessingException {
		TopicInfo topicInfo1 = mapper.readValue(Utils.readResourceAsString("topic-info.json"), TopicInfo.class);
		TopicInfo topicInfo2 = mapper.readValue(Utils.readResourceAsString("topic-info.json"), TopicInfo.class);

        assertEquals(topicInfo1, topicInfo2);

		var ta1 = new TopicAddressImpl(topicInfo1);
		var ta2 = new TopicAddressImpl(topicInfo2);

        assertEquals(ta1, ta2);
	}

	@Test
	public void testHashCode() throws JsonProcessingException {
		TopicInfo topicInfo1 = mapper.readValue(Utils.readResourceAsString("topic-info.json"), TopicInfo.class);
		var ta1 = new TopicAddressImpl(topicInfo1);

		assertEquals(1402967144, ta1.hashCode());

		TopicInfo topicInfo2 = mapper.readValue(Utils.readResourceAsString("topic-info.json"), TopicInfo.class);
		var ta2 = new TopicAddressImpl(topicInfo2);

		// check repeatability
		assertEquals(ta1.hashCode(), ta2.hashCode());
	}
}
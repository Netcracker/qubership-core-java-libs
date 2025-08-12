package com.netcracker.cloud.maas.client.impl.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netcracker.cloud.maas.client.impl.dto.kafka.v1.TopicInfo;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TopicInfoTest {

    @Test
    public void testDeserialization() throws Exception {
        String json = "{\n" +
                "   \"addresses\": {\n" +
                "       \"PLAINTEXT\": [\n" +
                "           \"localhost:9092\"\n" +
                "       ]\n" +
                "   },\n" +
                "   \"name\": \"maas.namespace.my-lazy-topic12.c3b9b7e8d5be4c4e9396d674b7293190\",\n" +
                "   \"classifier\": {\n" +
                "       \"name\": \"my-lazy-topic\",\n" +
                "       \"namespace\": \"my-lazy-topic\"\n" +
                "   },\n" +
                "   \"namespace\": \"namespace\",\n" +
                "   \"instance\": \"cpq-kafka-maas-test\",\n" +
                "   \"requestedSettings\": {\n" +
                "       \"numPartitions\": 1,\n" +
                "       \"replicationFactor\": 1,\n" +
                "       \"configs\": {\n" +
                "           \"flush.ms\": \"1088\"\n" +
                "       }\n" +
                "   },\n" +
                "   \"actualSettings\": {\n" +
                "       \"numPartitions\": 1,\n" +
                "       \"replicationFactor\": 1,\n" +
                "       \"replicaAssignment\": {\n" +
                "           \"0\": [\n" +
                "               0\n" +
                "           ]\n" +
                "       },\n" +
                "       \"configs\": {\n" +
                "           \"cleanup.policy\": \"delete\",\n" +
                "           \"compression.type\": \"producer\",\n" +
                "           \"delete.retention.ms\": \"86400000\",\n" +
                "           \"file.delete.delay.ms\": \"60000\",\n" +
                "           \"flush.messages\": \"9223372036854775807\",\n" +
                "           \"flush.ms\": \"1088\",\n" +
                "           \"follower.replication.throttled.replicas\": \"\",\n" +
                "           \"index.interval.bytes\": \"4096\",\n" +
                "           \"leader.replication.throttled.replicas\": \"\",\n" +
                "           \"max.compaction.lag.ms\": \"9223372036854775807\",\n" +
                "           \"max.message.bytes\": \"1048588\",\n" +
                "           \"message.downconversion.enable\": \"true\",\n" +
                "           \"message.format.version\": \"2.6-IV0\",\n" +
                "           \"message.timestamp.difference.max.ms\": \"9223372036854775807\",\n" +
                "           \"message.timestamp.type\": \"CreateTime\",\n" +
                "           \"min.cleanable.dirty.ratio\": \"0.5\",\n" +
                "           \"min.compaction.lag.ms\": \"0\",\n" +
                "           \"min.insync.replicas\": \"1\",\n" +
                "           \"preallocate\": \"false\",\n" +
                "           \"retention.bytes\": \"-1\",\n" +
                "           \"retention.ms\": \"604800000\",\n" +
                "           \"segment.bytes\": \"1073741824\",\n" +
                "           \"segment.index.bytes\": \"10485760\",\n" +
                "           \"segment.jitter.ms\": \"0\",\n" +
                "           \"segment.ms\": \"604800000\",\n" +
                "           \"unclean.leader.election.enable\": \"false\"\n" +
                "       }\n" +
                "   },\n" +
                "   \"template\": \"my-template444\"\n" +
                "}\n";

        TopicInfo info = new ObjectMapper().readValue(json.getBytes(StandardCharsets.UTF_8), TopicInfo.class);
        assertEquals("maas.namespace.my-lazy-topic12.c3b9b7e8d5be4c4e9396d674b7293190", info.getName());
    }

    @Test
    public void testDeserializationWithCreds() throws IOException {
        String json = "    {\n" +
                "        \"addresses\": {\n" +
                "            \"SASL_PLAINTEXT\": [\n" +
                "                \"kafka.cpq-kafka:9092\"\n" +
                "            ]\n" +
                "        },\n" +
                "        \"name\": \"maas.cloudbss311-platform-core-support-dev2.orders.fd511eca0e5b48aea993f8782d52a16e\",\n" +
                "        \"classifier\": {\n" +
                "            \"name\": \"orders\",\n" +
                "            \"namespace\": \"cloudbss311-platform-core-support-dev2\",\n" +
                "            \"tenantId\": \"9a47b338-9b7d-41a7-bdd4-c6804b69ab80\"\n" +
                "        },\n" +
                "        \"namespace\": \"cloudbss311-platform-core-support-dev2\",\n" +
                "        \"externallyManaged\": false,\n" +
                "        \"instance\": \"cpq-kafka-maas-test\",\n" +
                "        \"caCert\": \"MIID7jCCAtagAwI\",\n" +
                "        \"credential\": {\n" +
                "            \"client\": [\n" +
                "                {\n" +
                "                    \"password\": \"plain:client\",\n" +
                "                    \"type\": \"SCRAM\",\n" +
                "                    \"username\": \"client\",\n" +
                "                    \"clientCert\": \"MIID/TCCAuWgAwIBAgIQ\",\n" +
                "                    \"clientKey\": \"MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwg\"\n" +
                "                }\n" +
                "            ]\n" +
                "        },\n" +
                "        \"requestedSettings\": {\n" +
                "            \"numPartitions\": 1,\n" +
                "            \"replicationFactor\": 1\n" +
                "        },\n" +
                "        \"actualSettings\": {\n" +
                "            \"numPartitions\": 1,\n" +
                "            \"replicationFactor\": 1,\n" +
                "            \"replicaAssignment\": {\n" +
                "                \"0\": [\n" +
                "                    1\n" +
                "                ]\n" +
                "            },\n" +
                "            \"configs\": {\n" +
                "                \"cleanup.policy\": \"delete\",\n" +
                "                \"compression.type\": \"producer\",\n" +
                "                \"delete.retention.ms\": \"86400000\",\n" +
                "                \"file.delete.delay.ms\": \"60000\",\n" +
                "                \"flush.messages\": \"9223372036854775807\",\n" +
                "                \"flush.ms\": \"9223372036854775807\",\n" +
                "                \"follower.replication.throttled.replicas\": \"\",\n" +
                "                \"index.interval.bytes\": \"4096\",\n" +
                "                \"leader.replication.throttled.replicas\": \"\",\n" +
                "                \"max.message.bytes\": \"1000012\",\n" +
                "                \"message.downconversion.enable\": \"true\",\n" +
                "                \"message.format.version\": \"2.2-IV1\",\n" +
                "                \"message.timestamp.difference.max.ms\": \"9223372036854775807\",\n" +
                "                \"message.timestamp.type\": \"CreateTime\",\n" +
                "                \"min.cleanable.dirty.ratio\": \"0.5\",\n" +
                "                \"min.compaction.lag.ms\": \"0\",\n" +
                "                \"min.insync.replicas\": \"1\",\n" +
                "                \"preallocate\": \"false\",\n" +
                "                \"retention.bytes\": \"-1\",\n" +
                "                \"retention.ms\": \"604800000\",\n" +
                "                \"segment.bytes\": \"1073741824\",\n" +
                "                \"segment.index.bytes\": \"10485760\",\n" +
                "                \"segment.jitter.ms\": \"0\",\n" +
                "                \"segment.ms\": \"604800000\",\n" +
                "                \"unclean.leader.election.enable\": \"false\"\n" +
                "            }\n" +
                "        }\n" +
                "    }\n";
        TopicInfo info = new ObjectMapper().readValue(json.getBytes(StandardCharsets.UTF_8), TopicInfo.class);
        assertEquals("maas.cloudbss311-platform-core-support-dev2.orders.fd511eca0e5b48aea993f8782d52a16e", info.getName());
        assertEquals("MIID7jCCAtagAwI", info.getCaCert());
        assertEquals("MIID/TCCAuWgAwIBAgIQ", info.getCredential().get("client").get(0).getClientCert());
        assertEquals("MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwg", info.getCredential().get("client").get(0).getClientKey());
    }
}

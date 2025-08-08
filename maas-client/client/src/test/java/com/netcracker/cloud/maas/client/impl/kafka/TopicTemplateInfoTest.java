package com.netcracker.cloud.maas.client.impl.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.qubership.cloud.maas.client.impl.dto.kafka.v1.TopicSettings;
import org.qubership.cloud.maas.client.impl.dto.kafka.v1.TopicTemplateInfo;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TopicTemplateInfoTest {

    public static final String NEW_NAME = "new-name";
    public static final String NEW_NAMESPACE = "new-namespace";

    @Test
    void testFields() throws Exception {
        String json = "{\n" +
                "   \"name\": \"maas.namespace.my-lazy-topic12.c3b9b7e8d5be4c4e9396d674b7293190\",\n" +
                "   \"namespace\": \"namespace\",\n" +
                "   \"currentSettings\": {\n" +
                "       \"numPartitions\": 1,\n" +
                "       \"replicationFactor\": 1,\n" +
                "       \"configs\": {\n" +
                "           \"flush.ms\": \"1088\"\n" +
                "       }\n" +
                "   },\n" +
                "   \"previousSettings\": {\n" +
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
                "           \"segment.bytes\": \"1073741824\",\n" +
                "           \"segment.index.bytes\": \"10485760\",\n" +
                "           \"segment.jitter.ms\": \"0\",\n" +
                "           \"segment.ms\": \"604800000\",\n" +
                "           \"unclean.leader.election.enable\": \"false\"\n" +
                "       }\n" +
                "   }\n" +
                "}\n";


        TopicTemplateInfo info = new ObjectMapper().readValue(json.getBytes(StandardCharsets.UTF_8), TopicTemplateInfo.class);
        assertEquals("maas.namespace.my-lazy-topic12.c3b9b7e8d5be4c4e9396d674b7293190", info.getName());
        assertEquals("namespace", info.getNamespace());
        assertEquals(1, info.getCurrentSettings().getConfigs().size());
        assertEquals(11, info.getPreviousSettings().getConfigs().size());

        info.setName(NEW_NAME);
        info.setNamespace(NEW_NAMESPACE);
        info.setCurrentSettings(new TopicSettings());
        info.setPreviousSettings(new TopicSettings());
        assertEquals(NEW_NAME, info.getName());
        assertEquals(NEW_NAMESPACE, info.getNamespace());
        assertNull(info.getCurrentSettings().getConfigs());
        assertNull(info.getPreviousSettings().getConfigs());
    }

}

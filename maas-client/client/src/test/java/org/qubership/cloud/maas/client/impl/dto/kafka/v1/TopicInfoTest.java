package org.qubership.cloud.maas.client.impl.dto.kafka.v1;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.qubership.cloud.maas.client.Utils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TopicInfoTest {
    @Test
    void testDeserialization() throws JsonProcessingException {
        var json = Utils.readResourceAsString("TopicInfo.json");
        var om = new ObjectMapper();
        var topicInfo = om.readValue(json, TopicInfo.class);
        assertEquals(31, topicInfo.getActualSettings().getConfigs().size());
    }
}

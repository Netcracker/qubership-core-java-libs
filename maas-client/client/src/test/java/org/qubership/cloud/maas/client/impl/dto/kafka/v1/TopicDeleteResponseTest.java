package org.qubership.cloud.maas.client.impl.dto.kafka.v1;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class TopicDeleteResponseTest {
    @Test
    void testDeserialization() throws JsonProcessingException {
        ObjectMapper om = new ObjectMapper();
        TopicDeleteResponse topicDeleteResponse = om.readValue("{\"deletedSuccessfully\":[],\"failedToDelete\":[]}", TopicDeleteResponse.class);
        assertNotNull(topicDeleteResponse.getDeletedSuccessfully());
        assertNotNull(topicDeleteResponse.getFailedToDelete());
    }

    @Test
    void testDeserialization2() throws JsonProcessingException {
        ObjectMapper om = new ObjectMapper();
        TopicDeleteResponse topicDeleteResponse = om.readValue("{}", TopicDeleteResponse.class);
        assertNotNull(topicDeleteResponse.getDeletedSuccessfully());
        assertNotNull(topicDeleteResponse.getFailedToDelete());
    }
}
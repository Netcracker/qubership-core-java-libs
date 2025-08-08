package com.netcracker.maas.declarative.kafka.client.api.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MaasKafkaTopicDefinitionBuildExceptionTest {
    @Test
    void testExceptionMessageFormat() {
        var e = new MaasKafkaTopicDefinitionBuildException("oops, %s did %s again","I", "it");
        assertEquals("oops, I did it again", e.getMessage());
    }
}

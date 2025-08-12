package com.netcracker.maas.declarative.kafka.client.api.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MaasKafkaConsumerCreationRequestBuildExceptionTest {
    @Test
    void testExceptionMessageFormat() {
        var e = new MaasKafkaConsumerCreationRequestBuildException("oops, %s did %s again","I", "it");
        assertEquals("oops, I did it again", e.getMessage());
    }
}

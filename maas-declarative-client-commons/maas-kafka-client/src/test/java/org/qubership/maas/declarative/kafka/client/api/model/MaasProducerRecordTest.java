package org.qubership.maas.declarative.kafka.client.api.model;

import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.header.internals.RecordHeaders;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;


class MaasProducerRecordTest {
    @Test
    void testEquals1() {
        var one = new MaasProducerRecord<>(1, "order1", "data", 123L, new RecordHeaders().add(new RecordHeader("abc", "value".getBytes())));
        var two = new MaasProducerRecord<>(1, "order1", "data", 123L, new RecordHeaders().add(new RecordHeader("abc", "value".getBytes())));
        assertEquals(one, two);
    }

    @Test
    void testEquals2() {
        var one = new MaasProducerRecord<>(1, "order1", "data", 123L, new RecordHeaders().add(new RecordHeader("abc", "value".getBytes())));
        var two = new MaasProducerRecord<>(1, "order2", "data", 123L, new RecordHeaders().add(new RecordHeader("abc", "value".getBytes())));
        assertNotEquals(one, two);
    }

    @Test
    void testEquals3() {
        var one = new MaasProducerRecord<>(1, "order1", "data", 123L, new RecordHeaders().add(new RecordHeader("abc", "value".getBytes())));
        var two = new MaasProducerRecord<>(1, "order1", "data", 123L, new RecordHeaders().add(new RecordHeader("other", "nw".getBytes())));
        assertNotEquals(one, two);
    }

    @Test
    void testHashCode() {
        var one = new MaasProducerRecord<>(1, "order1", "data", 123L, new RecordHeaders().add(new RecordHeader("abc", "value".getBytes())));
        assertEquals(-1558503239, one.hashCode());
    }

    @Test
    void testToString1() {
        var one = new MaasProducerRecord<>(1, "order1", "data", 123L, new RecordHeaders().add(new RecordHeader("abc", "value".getBytes())));
        assertEquals("MaasProducerRecord[partition=1, key=order1, value=data, timestamp=123, headers=RecordHeaders(headers = [RecordHeader(key = abc, value = [118, 97, 108, 117, 101])], isReadOnly = false)]", one.toString());
    }

    @Test
    void testToString2() {
        var one = new MaasProducerRecord<>(null, null, null, null, null);
        assertEquals("MaasProducerRecord[partition=null, key=null, value=null, timestamp=null, headers=null]", one.toString());
    }
}
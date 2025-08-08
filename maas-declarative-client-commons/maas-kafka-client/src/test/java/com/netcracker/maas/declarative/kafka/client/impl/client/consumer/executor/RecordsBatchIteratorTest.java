package org.qubership.maas.declarative.kafka.client.impl.client.consumer.executor;

import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.Test;

import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

class RecordsBatchIteratorTest {
    @Test
    void testIterator() {
        var rg = new RecordsGenerator();

        var rbi = new RecordsBatchIterator<>();
        assertEquals("empty", rbi.toString());
        assertTrue(rbi.isProcessed());
        assertNull(rbi.record());
        rbi.moveToNextRecord(); // just try for safe execution in all cases
        assertEquals(0, rbi.handledRecords().size());

        Supplier<Long> cm = () -> rbi.getBatchCommitMarker().getPosition().get(new TopicPartition("orders", 0)).offset();

        rbi.set(rg.next().get());
        assertEquals("batch size: 2, position: 0, commit: {orders-0=OffsetAndMetadata{offset=2, leaderEpoch=null, metadata=''}}", rbi.toString());
        assertFalse(rbi.isProcessed());
        assertEquals("order1", rbi.record().getConsumerRecord().key());
        assertEquals("order1", rbi.record().getConsumerRecord().key());
        rbi.moveToNextRecord();
        assertEquals("order2", rbi.record().getConsumerRecord().key());
        assertEquals("order2", rbi.record().getConsumerRecord().key());
        rbi.moveToNextRecord();
        assertEquals(2, cm.get());
        assertFalse(rbi.isProcessed());
        rbi.markProcessed();
        assertTrue(rbi.isProcessed());

        rbi.set(rg.next().get());
        assertEquals("batch size: 2, position: 0, commit: {orders-0=OffsetAndMetadata{offset=4, leaderEpoch=null, metadata=''}}", rbi.toString());
        assertFalse(rbi.isProcessed());
        assertEquals(0, rbi.handledRecords().size());
        assertEquals("order3", rbi.record().getConsumerRecord().key());
        assertEquals("order3", rbi.record().getConsumerRecord().key());
        rbi.moveToNextRecord();
        assertEquals(1, rbi.handledRecords().size());
        assertEquals("order4", rbi.record().getConsumerRecord().key());
        assertEquals("order4", rbi.record().getConsumerRecord().key());
        rbi.moveToNextRecord();
        assertEquals(4, cm.get());
        assertFalse(rbi.isProcessed());
        rbi.markProcessed();
        assertTrue(rbi.isProcessed());

        // test batch with records filtered out by bg version filter
        rbi.set(rg.nextEmpty().get());
        assertEquals("batch size: 0, position: 0, commit: {orders-0=OffsetAndMetadata{offset=6, leaderEpoch=null, metadata=''}}", rbi.toString());
        assertFalse(rbi.isProcessed());
        assertNull(rbi.record());
        rbi.moveToNextRecord(); // just try for safe execution in all cases
        assertEquals(6, cm.get());
        assertFalse(rbi.isProcessed());
        rbi.markProcessed();
        assertTrue(rbi.isProcessed());
    }
}
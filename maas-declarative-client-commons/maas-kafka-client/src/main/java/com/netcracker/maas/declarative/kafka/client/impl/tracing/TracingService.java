package org.qubership.maas.declarative.kafka.client.impl.tracing;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;

public interface TracingService {
    Span buildSpanWithB3Context(ConsumerRecord consumerRecord);

    ProducerRecord addB3HeadersFromContext(ProducerRecord record);

    Scope activateSpan(Span span);
}

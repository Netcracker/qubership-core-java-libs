package com.netcracker.maas.declarative.kafka.client.api.filter;

import org.qubership.cloud.maas.bluegreen.kafka.Record;
import org.qubership.maas.declarative.kafka.client.impl.client.consumer.filter.Chain;

public interface RecordFilter {
    void doFilter(Record<?, ?> record, Chain<Record<?, ?>> next);

    int order();
}

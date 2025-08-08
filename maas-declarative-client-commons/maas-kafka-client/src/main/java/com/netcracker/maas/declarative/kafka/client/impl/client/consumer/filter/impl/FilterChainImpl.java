package com.netcracker.maas.declarative.kafka.client.impl.client.consumer.filter.impl;

import org.qubership.cloud.maas.bluegreen.kafka.Record;
import org.qubership.maas.declarative.kafka.client.api.filter.RecordFilter;
import org.qubership.maas.declarative.kafka.client.impl.client.consumer.filter.Chain;

public class FilterChainImpl implements Chain<Record<?, ?>> {
    private final RecordFilter impl;
    private final Chain<Record<?, ?>> next;

    FilterChainImpl(RecordFilter impl, Chain<Record<?, ?>> next) {
        this.impl = impl;
        this.next = next;
    }

    @Override
    public void doFilter(Record<?, ?> data) {
        impl.doFilter(data, next);
    }
}

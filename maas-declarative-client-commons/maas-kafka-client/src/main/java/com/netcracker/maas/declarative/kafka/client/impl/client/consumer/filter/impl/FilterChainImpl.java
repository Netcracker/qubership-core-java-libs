package com.netcracker.maas.declarative.kafka.client.impl.client.consumer.filter.impl;

import com.netcracker.cloud.maas.bluegreen.kafka.Record;
import com.netcracker.maas.declarative.kafka.client.api.filter.RecordFilter;
import com.netcracker.maas.declarative.kafka.client.impl.client.consumer.filter.Chain;

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

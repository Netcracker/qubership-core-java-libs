package com.netcracker.maas.declarative.kafka.client.impl.client.consumer.filter.impl;

import com.netcracker.cloud.maas.bluegreen.kafka.Record;
import com.netcracker.maas.declarative.kafka.client.api.filter.ConsumerRecordFilter;
import com.netcracker.maas.declarative.kafka.client.impl.client.consumer.filter.Chain;

public class NoopFilter implements ConsumerRecordFilter {

    public static final NoopFilter INSTANCE = new NoopFilter();

    @Override
    public void doFilter(Record<?, ?> rec, Chain<Record<?, ?>> next) {
        next.doFilter(rec);
    }

    @Override
    public int order() {
        return 0;
    }
}

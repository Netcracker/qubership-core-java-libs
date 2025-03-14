package org.qubership.maas.declarative.kafka.client.impl.client.consumer.filter.impl;

import org.qubership.cloud.maas.bluegreen.kafka.Record;
import org.qubership.maas.declarative.kafka.client.api.filter.ConsumerRecordFilter;
import org.qubership.maas.declarative.kafka.client.impl.client.consumer.filter.Chain;

public class NoopFilter implements ConsumerRecordFilter {

    public static final NoopFilter INSTANCE = new NoopFilter();

    @Override
    public void doFilter(Record<?, ?> rec, Chain<Record<?, ?>> next) {
        // NOOP
    }

    @Override
    public int order() {
        return 0;
    }
}

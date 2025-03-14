package org.qubership.maas.declarative.kafka.client.impl.client.consumer.filter.impl;

import org.qubership.cloud.maas.bluegreen.kafka.Record;
import org.qubership.maas.declarative.kafka.client.api.filter.RecordFilter;
import org.qubership.maas.declarative.kafka.client.impl.client.consumer.filter.Chain;

import java.util.List;

public class FilterExecutor {
    public static void execute(List<? extends RecordFilter> filters, Record<?, ?> rec) {
        if (filters == null || filters.isEmpty()) {
            return;
        }
        Chain<Record<?, ?>> chain = null;
        for (int i = filters.size() - 1; i >= 0; i--) {
            chain = new FilterChainImpl(filters.get(i), chain);
        }

        chain.doFilter(rec);
    }
} 
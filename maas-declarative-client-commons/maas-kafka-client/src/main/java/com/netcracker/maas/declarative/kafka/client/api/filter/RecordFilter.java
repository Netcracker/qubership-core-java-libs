package com.netcracker.maas.declarative.kafka.client.api.filter;

import com.netcracker.cloud.maas.bluegreen.kafka.Record;
import com.netcracker.maas.declarative.kafka.client.impl.client.consumer.filter.Chain;

public interface RecordFilter {
    void doFilter(Record<?, ?> record, Chain<Record<?, ?>> next);

    int order();
}

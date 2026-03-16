package com.netcracker.maas.declarative.kafka.client.impl.client.consumer.filter.impl;

import com.netcracker.cloud.maas.bluegreen.kafka.Record;
import com.netcracker.maas.declarative.kafka.client.api.filter.ConsumerRecordFilter;
import com.netcracker.maas.declarative.kafka.client.impl.client.consumer.filter.Chain;
import com.netcracker.maas.declarative.kafka.client.impl.tracing.Scope;
import com.netcracker.maas.declarative.kafka.client.impl.tracing.Span;
import com.netcracker.maas.declarative.kafka.client.impl.tracing.TracingService;

import static com.netcracker.maas.declarative.kafka.client.impl.client.consumer.filter.impl.ContextPropagationFilter.CONTEXT_PROPAGATION_ORDER;

public class TracingFilter implements ConsumerRecordFilter {

    private final TracingService tracingService;

    public static final int TRACING_ORDER = CONTEXT_PROPAGATION_ORDER - 1;

    public TracingFilter(TracingService tracingService) {
        this.tracingService = tracingService;
    }

    @Override
    public void doFilter(Record<?, ?> record, Chain<Record<?, ?>> next) {
        Span span = tracingService.buildSpanWithB3Context(record.getConsumerRecord());
        try (Scope ignored = tracingService.activateSpan(span)) {
            next.doFilter(record);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (span != null) {
                span.finish();
            }
        }
    }

    @Override
    public int order() {
        return TRACING_ORDER;
    }
}

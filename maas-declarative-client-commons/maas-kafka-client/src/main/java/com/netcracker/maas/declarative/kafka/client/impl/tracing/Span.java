package org.qubership.maas.declarative.kafka.client.impl.tracing;

public interface Span {

    Span NOOP_SPAN_INSTANCE = new Span() {
        @Override
        public void setIsError() {

        }

        @Override
        public void finish() {

        }
    };

    void setIsError();

    void finish();

}

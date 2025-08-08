package com.netcracker.cloud.dbaas.client.management.classifier;

import org.qubership.cloud.dbaas.client.management.DbaasDbClassifier;

public abstract class DbaaSChainClassifierBuilder implements DbaaSClassifierBuilder {
    private DbaasDbClassifier.Builder wrapped;

    private DbaaSChainClassifierBuilder next;

    protected DbaaSChainClassifierBuilder(DbaaSChainClassifierBuilder next) {
        this.next = next;
    }

    public DbaaSChainClassifierBuilder pass(DbaasDbClassifier.Builder wrapped) {
        this.wrapped = wrapped;
        return this;
    }

    public DbaaSChainClassifierBuilder withProperty(String name, Object value) {
        getWrapped().withProperty(name, value);
        return this;
    }

    public DbaaSChainClassifierBuilder withCustomKey(String key, Object value) {
        getWrapped().withCustomKey(key, value);
        return this;
    }

    protected DbaasDbClassifier.Builder getWrapped() {
        if (wrapped == null) {
            this.wrapped = new DbaasDbClassifier.Builder();
        }
        return wrapped;
    }

    @Override
    public DbaasDbClassifier build() { // This method should be overriden
        if (next == null) {
            return getWrapped().build();
        }
        return new DbaasDbClassifier.Builder()
                .withProperties(next.build().asMap())
                .withProperties(getWrapped().build().asMap())
                .build();
    }
}

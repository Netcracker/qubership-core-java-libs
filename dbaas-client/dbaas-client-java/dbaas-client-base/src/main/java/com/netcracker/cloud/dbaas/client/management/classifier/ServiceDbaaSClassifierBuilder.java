package org.qubership.cloud.dbaas.client.management.classifier;

import org.qubership.cloud.dbaas.client.management.DbaasDbClassifier;

import static org.qubership.cloud.dbaas.client.DbaasConst.*;

public class ServiceDbaaSClassifierBuilder extends DbaaSChainClassifierBuilder {

    public ServiceDbaaSClassifierBuilder(DbaaSChainClassifierBuilder next) {
        super(next);
    }

    public ServiceDbaaSClassifierBuilder() {
        this(null);
    }

    @Override
    public DbaasDbClassifier build() {
        return new DbaasDbClassifier.Builder()
                .withProperties(super.build().asMap())
                .withProperty(SCOPE, SERVICE)
                .build();
    }
}

package org.qubership.cloud.dbaas.common.classifier;

import org.qubership.cloud.dbaas.client.DbaasConst;
import org.qubership.cloud.dbaas.client.management.DbaasDbClassifier;
import org.qubership.cloud.dbaas.client.management.classifier.DbaaSChainClassifierBuilder;

import java.util.Map;

import static org.qubership.cloud.dbaas.client.DbaasConst.SCOPE;

public class ServiceClassifierBuilder extends DbaaSChainClassifierBuilder {

    public ServiceClassifierBuilder(Map<String, Object> primaryClassifier) {
        super(null);
        getWrapped().withProperties(primaryClassifier);
    }

    @Override
    public DbaasDbClassifier build() {
        return new DbaasDbClassifier.Builder()
                .withProperties(getWrapped().build().asMap())
                .withProperty(SCOPE, DbaasConst.SERVICE)
                .build();
    }
}

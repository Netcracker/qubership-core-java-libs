package org.qubership.cloud.core.quarkus.dbaas.datasource.classifier;

import org.qubership.cloud.dbaas.client.DbaasConst;
import org.qubership.cloud.dbaas.client.management.DbaasDbClassifier;
import org.qubership.cloud.dbaas.client.management.classifier.DbaaSChainClassifierBuilder;

import java.util.Map;

import static org.qubership.cloud.dbaas.client.DbaasConst.SCOPE;

@Deprecated(forRemoval = true)
public class MicroserviceClassifierBuilder extends DbaaSChainClassifierBuilder {

    public MicroserviceClassifierBuilder(Map<String, Object> primaryClassifier) {
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

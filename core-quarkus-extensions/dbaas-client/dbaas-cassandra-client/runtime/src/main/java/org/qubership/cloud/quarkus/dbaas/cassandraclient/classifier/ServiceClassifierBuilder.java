package org.qubership.cloud.quarkus.dbaas.cassandraclient.classifier;

import org.qubership.cloud.dbaas.client.management.DbaasDbClassifier;
import org.qubership.cloud.dbaas.client.management.classifier.DbaaSClassifierBuilder;

import java.util.Map;

import static org.qubership.cloud.dbaas.client.DbaasConst.SCOPE;
import static org.qubership.cloud.dbaas.client.DbaasConst.SERVICE;

public class ServiceClassifierBuilder implements DbaaSClassifierBuilder {
    private final Map<String, Object> primaryClassifier;

    public ServiceClassifierBuilder(Map<String, Object> primaryClassifier) {
        this.primaryClassifier = primaryClassifier;
    }

    @Override
    public DbaasDbClassifier build() {
        primaryClassifier.put(SCOPE, SERVICE);
        return new DbaasDbClassifier(primaryClassifier);
    }
}

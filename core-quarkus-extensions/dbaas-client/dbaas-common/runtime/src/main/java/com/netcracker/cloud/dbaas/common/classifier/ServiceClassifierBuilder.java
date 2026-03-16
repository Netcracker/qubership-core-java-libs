package com.netcracker.cloud.dbaas.common.classifier;

import com.netcracker.cloud.dbaas.client.DbaasConst;
import com.netcracker.cloud.dbaas.client.management.DbaasDbClassifier;
import com.netcracker.cloud.dbaas.client.management.classifier.DbaaSChainClassifierBuilder;

import java.util.Map;

import static com.netcracker.cloud.dbaas.client.DbaasConst.SCOPE;

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

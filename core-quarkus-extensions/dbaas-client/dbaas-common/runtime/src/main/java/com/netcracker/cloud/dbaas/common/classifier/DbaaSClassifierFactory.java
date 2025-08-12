package com.netcracker.cloud.dbaas.common.classifier;

import com.netcracker.cloud.dbaas.client.management.classifier.DbaaSClassifierBuilder;

import java.util.Map;

public class DbaaSClassifierFactory {
    public DbaaSClassifierBuilder newTenantClassifierBuilder(Map<String, Object> primaryClassifier) {
        return new TenantClassifierBuilder(primaryClassifier);
    }

    public DbaaSClassifierBuilder newServiceClassifierBuilder(Map<String, Object> primaryClassifier) {
        return new ServiceClassifierBuilder(primaryClassifier);
    }
}

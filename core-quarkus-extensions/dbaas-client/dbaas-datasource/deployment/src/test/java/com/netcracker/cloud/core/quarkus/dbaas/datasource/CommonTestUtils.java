package com.netcracker.cloud.core.quarkus.dbaas.datasource;

import com.netcracker.cloud.dbaas.client.management.DbaasDbClassifier;
import com.netcracker.cloud.dbaas.client.management.classifier.DbaaSClassifierBuilder;
import com.netcracker.cloud.dbaas.common.classifier.ServiceClassifierBuilder;

import java.util.HashMap;
import java.util.Map;

import static com.netcracker.cloud.dbaas.client.DbaasConst.SCOPE;
import static com.netcracker.cloud.dbaas.client.DbaasConst.SERVICE;

public class CommonTestUtils {

    public static final String TEST_NAMESPACE = "test-namespace";

    public static DbaasDbClassifier getServiceClassifier() {
        Map<String, Object> params = new HashMap<>();
        params.put("microserviceName", "test-service");
        params.put(SCOPE, SERVICE);
        return new DbaasDbClassifier(params);
    }

    public static DbaasDbClassifier getServiceClassifier(String logicalDbName) {
        return getServiceClassifierBuilder(logicalDbName).build();
    }

    public static DbaaSClassifierBuilder getServiceClassifierBuilder(String logicalDbName) {
        return new ServiceClassifierBuilder(getDefaultClassifierFields()).withCustomKey("logicalDbName", logicalDbName);
    }

    private static Map<String, Object> getDefaultClassifierFields() {
        return Map.of("microserviceName", "test-service");
    }

}

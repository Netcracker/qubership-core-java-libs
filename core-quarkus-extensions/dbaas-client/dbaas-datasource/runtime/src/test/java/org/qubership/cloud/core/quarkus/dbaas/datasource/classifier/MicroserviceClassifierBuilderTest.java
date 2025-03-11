package org.qubership.cloud.core.quarkus.dbaas.datasource.classifier;

import org.qubership.cloud.dbaas.client.management.DbaasDbClassifier;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.qubership.cloud.dbaas.client.DbaasConst.SCOPE;
import static org.qubership.cloud.dbaas.client.DbaasConst.SERVICE;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class MicroserviceClassifierBuilderTest {

    @Test
    void testWithCustomKey() {
        Map<String, Object> expected = getInitialClassifierMap();
        expected.put("customKeys", Map.of("logicalDbName", "configs"));
        expected.put(SCOPE, SERVICE);
        DbaasDbClassifier actualClassifier = new MicroserviceClassifierBuilder(getInitialClassifierMap()).withCustomKey("logicalDbName", "configs").build();
        assertEquals(expected, actualClassifier.asMap());
    }

    private Map<String, Object> getInitialClassifierMap() {
        Map<String, Object> params = new HashMap<>();
        params.put("microserviceName", "test-service");
        params.put("namespace", "test-namespace");
        return params;
    }
}
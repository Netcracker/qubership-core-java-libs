package org.qubership.cloud.core.quarkus.dbaas.datasource;

import org.qubership.cloud.dbaas.client.management.DbaasDbClassifier;

import java.util.HashMap;
import java.util.Map;

import static org.qubership.cloud.dbaas.client.DbaasConst.SCOPE;
import static org.qubership.cloud.dbaas.client.DbaasConst.SERVICE;

public class CommonTestUtils {

    public static final String TEST_NAMESPACE = "test-namespace";

    public static DbaasDbClassifier getServiceClassifier() {
        Map<String, Object> params = new HashMap<>();
        params.put("microserviceName", "test-service");
        params.put(SCOPE, SERVICE);
        return new DbaasDbClassifier(params);
    }

}

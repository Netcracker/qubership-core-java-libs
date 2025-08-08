package com.netcracker.cloud.dbaas.client;

import java.util.HashMap;
import java.util.Map;

import static com.netcracker.cloud.dbaas.client.DbaasConst.*;

public class TestUtil {

    public static Map<String, Object> buildServiceClassifier(String namespace, String microserviceName) {
        Map<String, Object> classifier = new HashMap<>();
        classifier.put(NAMESPACE, namespace);
        classifier.put(MICROSERVICE_NAME, microserviceName);
        classifier.put(SCOPE, SERVICE);
        return classifier;
    }

    public static Map<String, Object> buildTenantClassifier(String namespace, String microserviceName, String tenantId) {
        Map<String, Object> classifier = new HashMap<>();
        classifier.put(NAMESPACE, namespace);
        classifier.put(MICROSERVICE_NAME, microserviceName);
        classifier.put(SCOPE, TENANT);
        classifier.put(TENANT_ID, tenantId);
        return classifier;
    }
}

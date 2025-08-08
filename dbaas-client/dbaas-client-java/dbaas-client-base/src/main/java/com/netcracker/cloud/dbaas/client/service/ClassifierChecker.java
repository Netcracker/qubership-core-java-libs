package org.qubership.cloud.dbaas.client.service;

import org.qubership.cloud.dbaas.client.DbaasConst;
import org.qubership.cloud.dbaas.client.exceptions.DbaaSClassifierNotValidException;

import java.util.Map;

/**
 * <pre> <code>
 * "classifier":{
 *   "tenantId": "id",                //mandatory if you use tenant scope
 *   "microserviceName": "name" ,   //mandatory
 *   "scope": "tenant"|"service" ,  //mandatory
 *   "namespace" : "namespace",     //mandatory
 *   "custom_keys": "custom field"  //optional
 * }
 * </code></pre>
 */
public final class ClassifierChecker {

    private static final String VAL = " value";
    private static final String MUSTHAVE = " must have ";
    private static final String CLASSIFIER_TITLE = "classifier: ";

    public void check(Map<String, Object> classifier) {
        String scope = (String) classifier.get(DbaasConst.SCOPE);
        if (scope == null || scope.isEmpty()) {
            throw new DbaaSClassifierNotValidException(CLASSIFIER_TITLE + classifier + MUSTHAVE + DbaasConst.SCOPE + " value " +
                    "which can be either 'service' or 'tenant'. E.g. \"scope\":\"service\" or \"scope\":\"tenant\"");
        }
        if (scope.equals(DbaasConst.SERVICE)) {
            checkServiceClassifier(classifier);
        } else if (scope.equals(DbaasConst.TENANT)) {
            checkTenantClassifier(classifier);
        } else {
            throw new DbaaSClassifierNotValidException(CLASSIFIER_TITLE + classifier + MUSTHAVE + DbaasConst.SCOPE + " value " +
                    "which can be either 'service' or 'tenant'. E.g. \"scope\":\"service\" or \"scope\":\"tenant\"");
        }

    }

    private void checkServiceClassifier(Map<String, Object> classifier) {
        checkCommonPart(classifier);
    }

    private void checkCommonPart(Map<String, Object> classifier) {
        if (!classifier.containsKey(DbaasConst.MICROSERVICE_NAME)) {
            throw new DbaaSClassifierNotValidException(CLASSIFIER_TITLE + classifier + MUSTHAVE + DbaasConst.MICROSERVICE_NAME + VAL);
        }
        if (!classifier.containsKey(DbaasConst.NAMESPACE)) {
            throw new DbaaSClassifierNotValidException(CLASSIFIER_TITLE + classifier + MUSTHAVE + DbaasConst.NAMESPACE + VAL);
        }
    }

    private void checkTenantClassifier(Map<String, Object> classifier) {
        checkCommonPart(classifier);
        if (!classifier.containsKey(DbaasConst.TENANT_ID)) {
            throw new DbaaSClassifierNotValidException("tenant classifier: " + classifier + MUSTHAVE + DbaasConst.TENANT_ID + VAL);
        }
    }
}

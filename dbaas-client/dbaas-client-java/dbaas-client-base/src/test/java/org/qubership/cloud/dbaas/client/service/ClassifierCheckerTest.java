package org.qubership.cloud.dbaas.client.service;

import org.qubership.cloud.dbaas.client.DbaasConst;
import org.qubership.cloud.dbaas.client.exceptions.DbaaSClassifierNotValidException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.fail;


public class ClassifierCheckerTest {

    private ClassifierChecker classifierChecker = new ClassifierChecker();
    private Map<String, Object> incompleteClassifier = new HashMap<>();

    {
        incompleteClassifier.put(DbaasConst.NAMESPACE, "test-namespace");
        incompleteClassifier.put(DbaasConst.MICROSERVICE_NAME, "test-ms");
    }

    @Test
    public void classifierHasNoScope() {
        Map<String, Object> classifier = new HashMap<>();
        try {
            classifierChecker.check(classifier);
            fail();
        } catch (DbaaSClassifierNotValidException e) {
            Assertions.assertEquals("classifier: " + classifier + " must have " + DbaasConst.SCOPE + " value " +
                    "which can be either 'service' or 'tenant'. E.g. \"scope\":\"service\" or \"scope\":\"tenant\"", e.getMessage());
        }
    }

    @Test
    public void classifierHasNoCorrectScope() {
        Map<String, Object> classifier = new HashMap<>(incompleteClassifier);
        classifier.put(DbaasConst.SCOPE, "service-tenant");
        try {
            classifierChecker.check(classifier);
            fail();
        } catch (DbaaSClassifierNotValidException e) {
            Assertions.assertEquals("classifier: " + classifier + " must have " + DbaasConst.SCOPE + " value " +
                    "which can be either 'service' or 'tenant'. E.g. \"scope\":\"service\" or \"scope\":\"tenant\"", e.getMessage());
        }
    }

    @Test
    public void classifierHasNoNamespace() {
        Map<String, Object> classifier = new HashMap<>(incompleteClassifier);
        classifier.remove(DbaasConst.NAMESPACE);
        classifier.put(DbaasConst.SCOPE, DbaasConst.SERVICE);
        try {
            classifierChecker.check(classifier);
            fail();
        } catch (DbaaSClassifierNotValidException e) {
            Assertions.assertEquals("classifier: " + classifier + " must have " + DbaasConst.NAMESPACE + " value", e.getMessage());
        }
    }

    @Test
    public void classifierHasNoMicroserviceName() {
        Map<String, Object> classifier = new HashMap<>(incompleteClassifier);
        classifier.remove(DbaasConst.MICROSERVICE_NAME);
        classifier.put(DbaasConst.SCOPE, DbaasConst.SERVICE);
        try {
            classifierChecker.check(classifier);
            fail();
        } catch (DbaaSClassifierNotValidException e) {
            Assertions.assertEquals("classifier: " + classifier + " must have " + DbaasConst.MICROSERVICE_NAME + " value", e.getMessage());
        }
    }

    @Test
    public void classifierHasNoTenantId() {
        Map<String, Object> classifier = new HashMap<>(incompleteClassifier);
        classifier.put(DbaasConst.SCOPE, DbaasConst.TENANT);
        try {
            classifierChecker.check(classifier);
            fail();
        } catch (DbaaSClassifierNotValidException e) {
            Assertions.assertEquals("tenant classifier: " + classifier + " must have " + DbaasConst.TENANT_ID + " value", e.getMessage());
        }
    }

    @Test
    public void tenantClassifierIsValid() {
        Map<String, Object> classifier = new HashMap<>(incompleteClassifier);
        classifier.put(DbaasConst.SCOPE, DbaasConst.TENANT);
        classifier.put(DbaasConst.TENANT_ID, "123");
        classifierChecker.check(classifier);
    }

    @Test
    public void serviceClassifierIsValid() {
        Map<String, Object> classifier = new HashMap<>(incompleteClassifier);
        classifier.put(DbaasConst.SCOPE, DbaasConst.SERVICE);
        classifierChecker.check(classifier);
    }
}
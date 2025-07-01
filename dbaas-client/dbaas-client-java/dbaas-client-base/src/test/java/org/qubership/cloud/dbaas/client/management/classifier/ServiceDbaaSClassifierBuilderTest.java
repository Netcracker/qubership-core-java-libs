package org.qubership.cloud.dbaas.client.management.classifier;

import org.qubership.cloud.dbaas.client.management.DbaasDbClassifier;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.qubership.cloud.dbaas.client.DbaasConst.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ServiceDbaaSClassifierBuilderTest {

    @Test
    public void testIsServiceField() {
        Map<String, Object> classifier = new ServiceDbaaSClassifierBuilder(null).build().asMap();
        assertEquals(1, classifier.size());
        assertEquals(SERVICE, classifier.get(SCOPE));
    }

    @Test
    public void testNextBuilderInChainIsCalled() {
        final String testFieldKey = "test-field";
        final String testFieldValue = "test-field-value";

        DbaaSChainClassifierBuilder cl1 = new DbaaSChainClassifierBuilder(null) {
            @Override
            public DbaasDbClassifier build() {
                return new DbaasDbClassifier.Builder()
                        .withProperties(super.build().asMap())
                        .withProperty("asdasdasf", "testFieldValue")
                        .build();
            }
        };
        cl1.withProperty("a1", "b1");

        DbaaSChainClassifierBuilder cl2 = new DbaaSChainClassifierBuilder(cl1) {
            @Override
            public DbaasDbClassifier build() {
                return new DbaasDbClassifier.Builder()
                        .withProperties(super.build().asMap())
                        .withProperty(testFieldKey, testFieldValue)
                        .build();
            }
        };
        cl2.withProperty("a1","b2");

        ServiceDbaaSClassifierBuilder classifierd = new ServiceDbaaSClassifierBuilder(cl2);
        classifierd.withProperty("a", "b");

        Map<String, Object> classifier = classifierd.build().asMap();
        assertEquals(5, classifier.size());
        assertEquals(testFieldValue, classifier.get(testFieldKey));
        assertEquals("b2", classifier.get("a1"));
        assertEquals("b", classifier.get("a"));
        assertEquals(SERVICE, classifier.get(SCOPE));
    }

    @Test
    public void testAdditionalFields() {
        final ServiceDbaaSClassifierBuilder classifierBuilder = new ServiceDbaaSClassifierBuilder(null);
        classifierBuilder.withProperty("someField", "someValue");
        assertTrue(classifierBuilder.build().asMap().containsKey("someField"));
    }

    @Test
    void testWithCustomKeys() {
        final ServiceDbaaSClassifierBuilder classifierBuilder = new ServiceDbaaSClassifierBuilder(null);
        classifierBuilder.withCustomKey("someField", "someValue");
        Map<String, Object> classifierAsMap = classifierBuilder.build().asMap();
        assertTrue(classifierAsMap.containsKey(CUSTOM_KEYS));
        assertTrue(((Map<String, Object> )classifierAsMap.get(CUSTOM_KEYS)).containsKey("someField"));
        assertEquals("someValue", ((Map<String, Object> )classifierAsMap.get(CUSTOM_KEYS)).get("someField"));
    }

}

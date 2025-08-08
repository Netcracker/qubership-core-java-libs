package org.qubership.cloud.dbaas.client.management;


import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static org.qubership.cloud.dbaas.client.DbaasConst.TENANT_ID;
import static org.junit.jupiter.api.Assertions.*;

public class DbaasDbClassifierTest {

    @Test
    public void testExtendedDbaasClassifier() {
        assertNotNullDbaasClassifier(new DbaasDbClassifier.Builder()
                .withProperty(TENANT_ID, "testTenantId")
                .build());
        assertNotNullDbaasClassifier(new DbaasDbClassifier.Builder()
                .withProperty(TENANT_ID, null)
                .build());
        assertNotNullDbaasClassifier(new DbaasDbClassifier.Builder()
                .withProperty(TENANT_ID, null)
                .build());
        assertEquals(new DbaasDbClassifier.Builder()
                        .withProperty(TENANT_ID, null)
                        .build(),
                new DbaasDbClassifier.Builder()
                        .withProperty(TENANT_ID, null)
                        .build(), "Classifiers with the same input parameters must be equal");
        assertEquals(
                Objects.hashCode(new DbaasDbClassifier.Builder().withProperty(TENANT_ID, null).build()),
                Objects.hashCode(new DbaasDbClassifier.Builder().withProperty(TENANT_ID, null).build()),
                "Classifiers with the same input parameters must have equal hashcode");
    }

    private void assertNotNullDbaasClassifier(DbaasDbClassifier dbaasDbClassifier) {
        assertNotNull(dbaasDbClassifier);
    }

    @Test
    public void testInvalidInput_NullDbClassifier() throws NullPointerException {
        new DbaasDbClassifier.Builder()
                .withProperty(TENANT_ID, "testTenantId")
                .build();
    }

    @Test
    public void testBuildClassifierFromMap() throws NullPointerException {
        DbaasDbClassifier build = new DbaasDbClassifier.Builder()
                .withProperties(Collections.singletonMap("key", "value"))
                .build();
        assertEquals(Collections.singletonMap("key", "value"), build.asMap());
    }

    @Test
    void testBuildClassifierWithCustomKeys() {
        DbaasDbClassifier build = new DbaasDbClassifier.Builder()
                .withCustomKey("logicalDbName", "configs")
                .build();
        Map<String, String> customKeys = Map.of("logicalDbName", "configs");

        assertEquals(Collections.singletonMap("customKeys", customKeys), build.asMap());
    }

    @Test
    public void testInvalidInput_NullPropertyName() throws NullPointerException {
        assertThrows(NullPointerException.class,
                () -> {
                    new DbaasDbClassifier.Builder()
                            .withProperty(null, null)
                            .build();
                });
    }


    @Test
    public void testAsMap() {
        DbaasDbClassifier dbaasDbClassifier = new DbaasDbClassifier.Builder()
                .withProperty(TENANT_ID, "testTenantId")
                .build();
        Map<String, Object> expectedResult = new HashMap<>();
        expectedResult.put(TENANT_ID, "testTenantId");
        assertEquals(expectedResult, dbaasDbClassifier.asMap());
    }

}

package com.netcracker.cloud.dbaas.client.metrics;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import static com.netcracker.cloud.dbaas.client.DbaasConst.SCOPE;
import static com.netcracker.cloud.dbaas.client.DbaasConst.SERVICE;
import static com.netcracker.cloud.dbaas.client.metrics.DatabaseMetricProperties.CLASSIFIER_TAG_PREFIX;
import static com.netcracker.cloud.dbaas.client.metrics.DatabaseMetricProperties.ROLE_TAG;
import static org.junit.jupiter.api.Assertions.assertEquals;

class DatabaseMetricPropertiesTest {
    private static String TEST_DB_NAME = "test-database";
    private static String TEST_DB_ROLE = "test-role";

    @Test
    void testGetMetricTags() {
        SortedMap<String, Object> classifier = new TreeMap<>();
        classifier.put(SCOPE, SERVICE);

        String extraTagKey = "extra_tag";
        String extraTagValue = "extra_tag_value";
        Map<String, String> additionalTags = Map.of(extraTagKey, extraTagValue);
        DatabaseMetricProperties metricProperties = new DatabaseMetricProperties(TEST_DB_NAME, TEST_DB_ROLE, classifier, Collections.emptyMap(), additionalTags);
        Map<String, String> metricTags = metricProperties.getMetricTags();
        assertEquals(SERVICE, metricTags.get(CLASSIFIER_TAG_PREFIX + SCOPE));
        assertEquals(TEST_DB_ROLE, metricTags.get(ROLE_TAG));
        assertEquals(extraTagValue, metricTags.get(extraTagKey));
    }
}

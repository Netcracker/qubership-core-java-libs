package com.netcracker.cloud.dbaas.client.metrics;

import lombok.*;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;

@Data
@AllArgsConstructor
@Builder
public class DatabaseMetricProperties {
    public static final String CLASSIFIER_TAG_PREFIX = "cl_";
    public static final String ROLE_TAG = "role";

    private String databaseName;
    @NonNull
    private String role;
    @NonNull
    private SortedMap<String, Object> classifier;
    private Map<String, Object> extraParameters;
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @Builder.Default
    private Map<String, String> additionalTags = new HashMap<>();

    public Map<String, String> getMetricTags() {
        Map<String, String> tags = new HashMap<>(additionalTags);
        for (Map.Entry<String, Object> classifierField : classifier.entrySet()) {
            tags.put(CLASSIFIER_TAG_PREFIX + classifierField.getKey(), String.valueOf(classifierField.getValue()));
        }
        tags.put(ROLE_TAG, role);
        return tags;
    }
}

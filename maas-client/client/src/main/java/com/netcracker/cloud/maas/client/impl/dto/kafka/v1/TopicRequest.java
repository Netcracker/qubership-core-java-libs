package org.qubership.cloud.maas.client.impl.dto.kafka.v1;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.qubership.cloud.maas.client.api.Classifier;
import org.qubership.cloud.maas.client.api.kafka.TopicCreateOptions;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

import java.util.List;
import java.util.Map;

@Data
@Builder(builderMethodName = "")
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@Jacksonized
public class TopicRequest extends TopicSettings {
    /**
     * Classifier partial field match
     */
    private Classifier classifier;

    /**
     * Include instance id in search clause
     */
    @Deprecated(since = "4.0.0", forRemoval = true)
    private String instance;

    String name;
    int numPartitions;
    int minNumPartitions;
    String replicationFactor;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    Map<String, List<Integer>> replicaAssignment;
    boolean externallyManaged;
    String template;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    Map<String, String> configs;
    boolean versioned;

    // this line needed as WA to javadoc pass
    public static class TopicRequestBuilder {}

    // override lombok builder to force user to specify mandatory parameters
    public static TopicRequestBuilder builder(Classifier classifier) {
        return new TopicRequestBuilder().classifier(classifier);
    }

    public TopicRequest options(TopicCreateOptions options) {
        name = options.getName();
        numPartitions = options.getNumPartitions();
        minNumPartitions = options.getMinNumPartitions();
        replicationFactor = options.getReplicationFactor();
        if (!options.getReplicaAssignment().isEmpty()) {
            replicaAssignment = options.getReplicaAssignment();
        }
        externallyManaged = options.isExternallyManaged();
        template = options.getTemplate();
        if(!options.getConfigs().isEmpty()) {
            configs = options.getConfigs();
        }
        versioned = options.isVersioned();
        return this;
    }
}

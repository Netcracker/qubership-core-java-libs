package com.netcracker.maas.declarative.kafka.client.api.model.definition;

import com.netcracker.maas.declarative.kafka.client.api.exception.MaasKafkaTopicDefinitionBuildException;
import lombok.*;

import java.util.Map;

@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
@Getter
public class MaasTopicDefinition {

    private String actualName;
    private String name;
    private String template; // maas template
    @Setter
    private String onTopicExist; // maas merge topic strategy (can be null)
    private String namespace;
    private Integer partitions;// may be null
    private String replicationFactor;// may be null
    private ManagedBy managedBy;
    private Map<String, Object> configs;// may be null
    private boolean versioned;

    // Definition builder
    public static class Builder {
        private static final String ERROR_MESSAGE_TEMPLATE = "Mandatory attribute '%s' can't be null";

        private MaasTopicDefinition topicDefinition;

        public Builder() {
            topicDefinition = new MaasTopicDefinition();
        }

        public Builder(MaasTopicDefinition topicDefinition) {
            this.topicDefinition = new MaasTopicDefinition(
                    topicDefinition.actualName,
                    topicDefinition.name,
                    topicDefinition.template,
                    topicDefinition.onTopicExist,
                    topicDefinition.namespace,
                    topicDefinition.partitions,
                    topicDefinition.replicationFactor,
                    topicDefinition.managedBy,
                    topicDefinition.configs,
                    topicDefinition.versioned
            );
        }

        public Builder setActualName(String actualName) {
            topicDefinition.actualName = actualName;
            return this;
        }

        public Builder setName(String name) {
            topicDefinition.name = name;
            return this;
        }

        public Builder setTemplate(String template) {
            topicDefinition.template = template;
            return this;
        }

        public Builder setOnTopicExist(String onTopicExist) {
            topicDefinition.onTopicExist = onTopicExist;
            return this;
        }

        public Builder setNamespace(String namespace) {
            topicDefinition.namespace = namespace;
            return this;
        }

        public Builder setPartitions(Integer partitions) {
            topicDefinition.partitions = partitions;
            return this;
        }

        public Builder setReplicationFactor(String replicationFactor) {
            topicDefinition.replicationFactor = replicationFactor;
            return this;
        }

        public Builder setManagedBy(ManagedBy managedBy) {
            topicDefinition.managedBy = managedBy;
            return this;
        }

        public Builder setConfigs(Map<String, Object> configs) {
            topicDefinition.configs = configs;
            return this;
        }

        public Builder setVersioned(boolean versioned) {
            topicDefinition.versioned = versioned;
            return this;
        }

        public MaasTopicDefinition build() {
            if (topicDefinition.name == null) {
                throw new MaasKafkaTopicDefinitionBuildException(ERROR_MESSAGE_TEMPLATE, "name");
            }
            if (topicDefinition.namespace == null) {
                throw new MaasKafkaTopicDefinitionBuildException(ERROR_MESSAGE_TEMPLATE, "namespace");
            }
            if (topicDefinition.managedBy == null) {
                throw new MaasKafkaTopicDefinitionBuildException(ERROR_MESSAGE_TEMPLATE, "managedBy");
            }

            return new MaasTopicDefinition(
                    topicDefinition.actualName,
                    topicDefinition.name,
                    topicDefinition.template,
                    topicDefinition.onTopicExist,
                    topicDefinition.namespace,
                    topicDefinition.partitions,
                    topicDefinition.replicationFactor,
                    topicDefinition.managedBy,
                    topicDefinition.configs,
                    topicDefinition.versioned
            );
        }
    }

    // Utils methods

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(MaasTopicDefinition topicDefinition) {
        return new Builder(topicDefinition);
    }


}

package com.netcracker.maas.declarative.kafka.client.api.model.definition;

import com.netcracker.maas.declarative.kafka.client.api.exception.MaasKafkaConsumerDefinitionBuildException;
import lombok.Getter;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.Map;

@Getter
public class MaasKafkaConsumerDefinition extends MaasKafkaCommonClientDefinition {
    private int instanceCount;// Will be used later
    private Integer pollDuration;
    private String groupId;
    private MaasKafkaBlueGreenDefinition blueGreenDefinition;
    private final Integer dedicatedThreadPoolSize;

    private MaasKafkaConsumerDefinition(
            MaasTopicDefinition topic,
            boolean isTenant,
            Map<String, Object> consumerConfig,
            int instanceCount,
            Integer pollDuration,
            String groupId,
            MaasKafkaBlueGreenDefinition blueGreenDefinition,
            Integer dedicatedThreadPoolSize
    ) {
        super(topic, isTenant, consumerConfig);
        this.instanceCount = instanceCount;
        this.pollDuration = pollDuration;
        this.groupId = groupId;
        this.blueGreenDefinition = blueGreenDefinition;
        this.dedicatedThreadPoolSize = dedicatedThreadPoolSize;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("instanceCount", instanceCount)
                .append("poolDuration", pollDuration)
                .append("groupId", groupId)
                .toString();
    }


    // Definition builder
    public static class Builder {
        private static final String ERROR_MESSAGE_TEMPLATE = "Mandatory attribute %s can't be null";

        private MaasTopicDefinition topic;
        private boolean isTenant;
        private Map<String, Object> consumerConfig;
        private int instanceCount;
        private Integer pollDuration;
        private String groupId;
        private MaasKafkaBlueGreenDefinition blueGreenDefinition;
        private Integer dedicatedThreadPoolSize;

        private Builder() {
        }

        private Builder(MaasKafkaConsumerDefinition consumerDefinition) {
            this.topic = consumerDefinition.getTopic();
            this.isTenant = consumerDefinition.isTenant();
            this.consumerConfig = consumerDefinition.getClientConfig();
            this.instanceCount = consumerDefinition.getInstanceCount();
            this.pollDuration = consumerDefinition.getPollDuration();
            this.groupId = consumerDefinition.getGroupId();
            this.dedicatedThreadPoolSize = consumerDefinition.getDedicatedThreadPoolSize();
        }

        public Builder setTopic(MaasTopicDefinition topic) {
            this.topic = topic;
            return this;
        }

        public Builder setTenant(boolean tenant) {
            isTenant = tenant;
            return this;
        }

        public Builder setClientConfig(Map<String, Object> consumerConfig) {
            this.consumerConfig = consumerConfig;
            return this;
        }

        public Builder setInstanceCount(int instanceCount) {
            this.instanceCount = instanceCount;
            return this;
        }

        public Builder setPollDuration(Integer pollDuration) {
            this.pollDuration = pollDuration;
            return this;
        }

        public Builder setGroupId(String groupId) {
            this.groupId = groupId;
            return this;
        }

        public Builder setBlueGreenDefinition(MaasKafkaBlueGreenDefinition blueGreenDefinition) {
            this.blueGreenDefinition = blueGreenDefinition;
            return this;
        }

        public Builder setDedicatedThreadPoolSize(Integer dedicatedThreadPoolSize) {
            this.dedicatedThreadPoolSize = dedicatedThreadPoolSize;
            return this;
        }

        public MaasKafkaConsumerDefinition build() {
            if (topic == null) {
                throw new MaasKafkaConsumerDefinitionBuildException(ERROR_MESSAGE_TEMPLATE, "topic");
            }
            if (groupId == null) {
                throw new MaasKafkaConsumerDefinitionBuildException(ERROR_MESSAGE_TEMPLATE, "groupId");
            }

            return new MaasKafkaConsumerDefinition(
                    topic,
                    isTenant,
                    consumerConfig,
                    instanceCount,
                    pollDuration,
                    groupId,
                    blueGreenDefinition,
                    dedicatedThreadPoolSize
            );
        }
    }

    // Utils methods
    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(MaasKafkaConsumerDefinition consumerDefinition) {
        return new Builder(consumerDefinition);
    }

}

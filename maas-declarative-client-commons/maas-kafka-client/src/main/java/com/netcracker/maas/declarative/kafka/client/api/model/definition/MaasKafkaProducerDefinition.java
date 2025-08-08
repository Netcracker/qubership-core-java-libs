package com.netcracker.maas.declarative.kafka.client.api.model.definition;


import com.netcracker.maas.declarative.kafka.client.api.exception.MaasKafkaProducerDefinitionBuildException;

import java.util.Map;

public class MaasKafkaProducerDefinition extends MaasKafkaCommonClientDefinition {

    private MaasKafkaProducerDefinition(
            MaasTopicDefinition topic,
            boolean isTenant,
            Map<String, Object> producerConfig
    ) {
        super(topic, isTenant, producerConfig);
    }

    // Definition builder
    public static class Builder {
        private static final String ERROR_MESSAGE_TEMPLATE = "Mandatory attribute %s can't be null";

        private MaasTopicDefinition topic;
        private boolean isTenant;
        private Map<String, Object> producerConfig;

        private Builder() {
        }

        private Builder(MaasKafkaProducerDefinition producerDefinition) {
            this.topic = producerDefinition.getTopic();
            this.isTenant = producerDefinition.isTenant();
            this.producerConfig = producerDefinition.getClientConfig();
        }

        public Builder setTopic(MaasTopicDefinition topic) {
            this.topic = topic;
            return this;
        }

        public Builder setTenant(boolean tenant) {
            isTenant = tenant;
            return this;
        }

        public Builder setClientConfig(Map<String, Object> producerConfig) {
            this.producerConfig = producerConfig;
            return this;
        }

        public MaasKafkaProducerDefinition build() {
            if (topic == null) {
                throw new MaasKafkaProducerDefinitionBuildException(String.format(ERROR_MESSAGE_TEMPLATE, "topic"));
            }

            return new MaasKafkaProducerDefinition(
                    topic,
                    isTenant,
                    producerConfig
            );
        }

    }

    // Utils methods

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(MaasKafkaProducerDefinition producerDefinition) {
        return new Builder(producerDefinition);
    }
}

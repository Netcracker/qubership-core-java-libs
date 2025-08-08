package com.netcracker.maas.declarative.kafka.client.api.model;

import com.netcracker.maas.declarative.kafka.client.api.exception.MaasKafkaProducerCreationRequestBuildException;
import com.netcracker.maas.declarative.kafka.client.api.model.definition.MaasKafkaProducerDefinition;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.Serializer;

import java.util.Map;
import java.util.function.Function;

public class MaasKafkaProducerCreationRequest {

    private MaasKafkaProducerDefinition producerDefinition;
    private Serializer keySerializer;
    private Serializer valueSerializer;
    private Function handler;

    private MaasKafkaProducerCreationRequest() {
    }

    public MaasKafkaProducerCreationRequest(
            MaasKafkaProducerDefinition producerDefinition,
            Serializer keySerializer,
            Serializer valueSerializer,
            Function handler
    ) {
        this.producerDefinition = producerDefinition;
        this.keySerializer = keySerializer;
        this.valueSerializer = valueSerializer;
        this.handler = handler;
    }

    public Function getHandler() {
        return handler;
    }

    public MaasKafkaProducerDefinition getProducerDefinition() {
        return producerDefinition;
    }

    public Serializer getKeySerializer() {
        return keySerializer;
    }

    public Serializer getValueSerializer() {
        return valueSerializer;
    }


    // Request builder
    public static final class ProducerRequestBuilder {
        private static final String NO_DEFINITION_ERROR_MESSAGE = "MaasKafkaProducerDefinition can't be null";
        private static final String NO_KEY_SERIALIZER_ERROR_MESSAGE_TEMPLATE =
                "There is no key serializer for producer %s (Key serializer should be filled" +
                        " in MaasKafkaProducerCreationRequest using ProducerRequestBuilder#setKeySerializer or specified in " +
                        "MaasKafkaProducerDefinition.clientConfiguration with key \"key.serializer\")";
        private static final String NO_VALUE_SERIALIZER_ERROR_MESSAGE_TEMPLATE =
                "There is no value serializer for producer %s (Value serializer should be filled" +
                        " in MaasKafkaProducerCreationRequest using ProducerRequestBuilder#setValueSerializer or specified in " +
                        "MaasKafkaProducerDefinition.clientConfiguration with key \"value.serializer\")";

        private final MaasKafkaProducerCreationRequest creationRequest;

        private ProducerRequestBuilder() {
            creationRequest = new MaasKafkaProducerCreationRequest();
        }

        public ProducerRequestBuilder setProducerDefinition(MaasKafkaProducerDefinition producerDefinition) {
            creationRequest.producerDefinition = producerDefinition;
            return this;
        }

        public ProducerRequestBuilder setKeySerializer(Serializer keySerializer) {
            creationRequest.keySerializer = keySerializer;
            return this;
        }

        public ProducerRequestBuilder setValueSerializer(Serializer valueSerializer) {
            creationRequest.valueSerializer = valueSerializer;
            return this;
        }

        public <K, V> ProducerRequestBuilder setHandler(Function<ProducerRecord<K, V>, ProducerRecord<K, V>> handler) {
            creationRequest.handler = handler;
            return this;
        }

        public MaasKafkaProducerCreationRequest build() {
            if (creationRequest.getProducerDefinition() == null) {
                throw new MaasKafkaProducerCreationRequestBuildException(NO_DEFINITION_ERROR_MESSAGE);
            }
            // Validate serializers
            if (creationRequest.keySerializer == null) {
                Map<String, Object> consumerConfig = creationRequest.producerDefinition.getClientConfig();
                if (!consumerConfig.containsKey("key.serializer")) {
                    throw new MaasKafkaProducerCreationRequestBuildException(
                            NO_KEY_SERIALIZER_ERROR_MESSAGE_TEMPLATE, creationRequest.producerDefinition);
                }
            }
            if (creationRequest.valueSerializer == null) {
                Map<String, Object> consumerConfig = creationRequest.producerDefinition.getClientConfig();
                if (!consumerConfig.containsKey("value.serializer")) {
                    throw new MaasKafkaProducerCreationRequestBuildException(
                            NO_VALUE_SERIALIZER_ERROR_MESSAGE_TEMPLATE, creationRequest.producerDefinition);
                }
            }

            return new MaasKafkaProducerCreationRequest(
                    creationRequest.producerDefinition,
                    creationRequest.keySerializer,
                    creationRequest.valueSerializer,
                    creationRequest.handler
            );
        }
    }

    // Utils methods

    public static ProducerRequestBuilder builder() {
        return new ProducerRequestBuilder();
    }
}

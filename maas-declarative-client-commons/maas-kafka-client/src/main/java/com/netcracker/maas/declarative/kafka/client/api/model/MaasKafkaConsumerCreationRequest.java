package com.netcracker.maas.declarative.kafka.client.api.model;

import com.netcracker.maas.declarative.kafka.client.api.MaasKafkaConsumerErrorHandler;
import com.netcracker.maas.declarative.kafka.client.api.exception.MaasKafkaConsumerCreationRequestBuildException;
import com.netcracker.maas.declarative.kafka.client.api.model.definition.MaasKafkaConsumerDefinition;
import lombok.Getter;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.Deserializer;

import java.util.Map;
import java.util.function.Consumer;

@Getter
public class MaasKafkaConsumerCreationRequest {
    private MaasKafkaConsumerDefinition consumerDefinition;
    private Deserializer keyDeserializer;
    private Deserializer valueDeserializer;
    private Consumer handler;
    private MaasKafkaConsumerErrorHandler errorHandler;
    private boolean customProcessed = false;

    private MaasKafkaConsumerCreationRequest() {
    }

    private MaasKafkaConsumerCreationRequest(
            MaasKafkaConsumerDefinition consumerDefinition,
            Deserializer keyDeserializer,
            Deserializer valueDeserializer,
            Consumer handler,
            MaasKafkaConsumerErrorHandler errorHandler,
            boolean customProcessed
    ) {
        this.consumerDefinition = consumerDefinition;
        this.keyDeserializer = keyDeserializer;
        this.valueDeserializer = valueDeserializer;
        this.handler = handler;
        this.errorHandler = errorHandler;
        this.customProcessed = customProcessed;
    }

    // Request builder
    public static final class ConsumerRequestBuilder {
        private static final String NO_DEFINITION_ERROR_MESSAGE = "MaasKafkaConsumerDefinition can't be null";
        private static final String NO_RECORD_HANDLER_ERROR_MESSAGE = "Record handler can't be null";
        private static final String NO_KEY_DESERIALIZER_ERROR_MESSAGE_TEMPLATE =
                "There is no key deserializer for consumer %s (Key deserializer should be filled" +
                        " in MaasKafkaConsumerCreationRequest using ConsumerRequestBuilder#setKeyDeserializer or specified in " +
                        "MaasKafkaConsumerDefinition.clientConfiguration with key \"key.deserializer\")";
        private static final String NO_VALUE_DESERIALIZER_ERROR_MESSAGE_TEMPLATE =
                "There is no value deserializer for consumer %s (Key deserializer should be filled" +
                        " in MaasKafkaConsumerCreationRequest using ConsumerRequestBuilder#setKeyDeserializer or specified in " +
                        "MaasKafkaConsumerDefinition.clientConfiguration with key \"value.deserializer\")";

        private final MaasKafkaConsumerCreationRequest creationRequest;

        private ConsumerRequestBuilder() {
            creationRequest = new MaasKafkaConsumerCreationRequest();
        }

        public ConsumerRequestBuilder setConsumerDefinition(MaasKafkaConsumerDefinition consumerDefinition) {
            creationRequest.consumerDefinition = consumerDefinition;
            return this;
        }

        public <K, V> ConsumerRequestBuilder setHandler(Consumer<ConsumerRecord<K, V>> handler) {
            creationRequest.handler = handler;
            return this;
        }

        public ConsumerRequestBuilder setErrorHandler(MaasKafkaConsumerErrorHandler errorHandler) {
            creationRequest.errorHandler = errorHandler;
            return this;
        }

        public ConsumerRequestBuilder setKeyDeserializer(Deserializer keyDeserializer) {
            creationRequest.keyDeserializer = keyDeserializer;
            return this;
        }

        public ConsumerRequestBuilder setValueDeserializer(Deserializer valueDeserializer) {
            creationRequest.valueDeserializer = valueDeserializer;
            return this;
        }

        public ConsumerRequestBuilder setCustomProcessed(boolean customProcessed) {
            creationRequest.customProcessed = customProcessed;
            return this;
        }

        public MaasKafkaConsumerCreationRequest build() {
            if (creationRequest.getConsumerDefinition() == null) {
                throw new MaasKafkaConsumerCreationRequestBuildException(NO_DEFINITION_ERROR_MESSAGE);
            }
            if (creationRequest.getHandler() == null) {
                throw new MaasKafkaConsumerCreationRequestBuildException(NO_RECORD_HANDLER_ERROR_MESSAGE);
            }
            // Validate deserializers
            if (creationRequest.keyDeserializer == null) {
                Map<String, Object> consumerConfig = creationRequest.consumerDefinition.getClientConfig();
                if (!consumerConfig.containsKey("key.deserializer")) {
                    throw new MaasKafkaConsumerCreationRequestBuildException(
                            NO_KEY_DESERIALIZER_ERROR_MESSAGE_TEMPLATE, creationRequest.consumerDefinition);
                }
            }
            if (creationRequest.valueDeserializer == null) {
                Map<String, Object> consumerConfig = creationRequest.consumerDefinition.getClientConfig();
                if (!consumerConfig.containsKey("value.deserializer")) {
                    throw new MaasKafkaConsumerCreationRequestBuildException(
                            NO_VALUE_DESERIALIZER_ERROR_MESSAGE_TEMPLATE, creationRequest.consumerDefinition);
                }
            }

            return new MaasKafkaConsumerCreationRequest(
                    creationRequest.consumerDefinition,
                    creationRequest.keyDeserializer,
                    creationRequest.valueDeserializer,
                    creationRequest.handler,
                    creationRequest.errorHandler,
                    creationRequest.customProcessed
            );
        }
    }

    // Utils methods

    public static ConsumerRequestBuilder builder() {
        return new ConsumerRequestBuilder();
    }
}

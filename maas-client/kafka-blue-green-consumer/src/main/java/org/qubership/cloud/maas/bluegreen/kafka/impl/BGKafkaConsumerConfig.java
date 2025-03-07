package org.qubership.cloud.maas.bluegreen.kafka.impl;

import org.qubership.cloud.bluegreen.api.service.BlueGreenStatePublisher;
import org.qubership.cloud.bluegreen.impl.service.InMemoryBlueGreenStatePublisher;
import org.qubership.cloud.maas.bluegreen.kafka.ConsumerConsistencyMode;
import org.qubership.cloud.maas.bluegreen.kafka.OffsetSetupStrategy;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.Deserializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.apache.kafka.clients.consumer.ConsumerConfig.GROUP_ID_CONFIG;

/**
 * Configuration class to set up BGKafkaConsumer
 * <p>
 * For local dev scenario, statePublisher field should be set to instance of {@link InMemoryBlueGreenStatePublisher}
 */
@Getter
@Setter
@Slf4j
public class BGKafkaConsumerConfig {
    Map<String, Object> properties;
    Set<String> topics;
    String groupIdPrefix;
    ConsumerConsistencyMode consistencyMode = ConsumerConsistencyMode.EVENTUAL;
    boolean ignoreFilter = false;
    OffsetSetupStrategy activeOffsetSetupStrategy = OffsetSetupStrategy.rewind(Duration.ofMinutes(5));
    OffsetSetupStrategy candidateOffsetSetupStrategy = OffsetSetupStrategy.rewind(Duration.ofMinutes(5));
    Function<Map<String, Object>, Consumer> consumerSupplier;
    Function<Map<String, Object>, AdminAdapter> adminAdapterSupplier;
    BlueGreenStatePublisher statePublisher;

    public static class Builder {
        BGKafkaConsumerConfig config;

        public Builder(BGKafkaConsumerConfig config) {
            this.config = config;
        }

        /**
         * Explicitly specify deserializer instances instead specifying via properties file
         *
         * @param keyDeserializer   instance for key deserializer
         * @param valueDeserializer instance for value deserializer
         * @return builder instance
         */
        public Builder deserializers(Deserializer keyDeserializer, Deserializer valueDeserializer) {
            consumerSupplier(props -> new KafkaConsumer(props, keyDeserializer, valueDeserializer));
            return this;
        }

        /**
         * Ignore version header and consume all messages without filtering
         *
         * @param consumerAll true - to ignore filtering, false - default behavior with message filtering according microservice BG version
         * @return builder instance
         */
        public Builder ignoreFilter(boolean consumerAll) {
            config.ignoreFilter = consumerAll;
            return this;
        }


        /**
         * Provide custom kafka consumer builder
         *
         * @param supplier kafka consumer instance creator
         * @return builder instance
         */
        public Builder consumerSupplier(Function<Map<String, Object>, Consumer> supplier) {
            config.consumerSupplier = supplier;
            return this;
        }

        /**
         * Provide custom kafka AdminAdapter builder
         *
         * @param supplier kafka AdminAdapter instance creator
         * @return builder instance
         */
        public Builder adminAdapterSupplier(Function<Map<String, Object>, AdminAdapter> supplier) {
            config.adminAdapterSupplier = supplier;
            return this;
        }

        /**
         * Set message consuming consistency mode: {@link ConsumerConsistencyMode#EVENTUAL}, {@link ConsumerConsistencyMode#GUARANTEE_CONSUMPTION}<br>
         * Defaults: {@link ConsumerConsistencyMode#EVENTUAL}
         *
         * @param mode consistency mode enum
         * @return builder instance
         */
        public Builder consistencyMode(ConsumerConsistencyMode mode) {
            config.consistencyMode = mode;
            return this;
        }

        /**
         * Set initial offset setup position for non-existing group id
         *
         * @param strategy strategy value. Defaults: {@code OffsetSetupStrategy.rewind(Duration.ofMinutes(5))}
         * @return builder instance
         */
        public Builder activeOffsetSetupStrategy(OffsetSetupStrategy strategy) {
            config.activeOffsetSetupStrategy = strategy;
            return this;
        }

        /**
         * Set initial offset setup position for non-existing group id
         *
         * @param strategy strategy value. Defaults: {@code OffsetSetupStrategy.rewind(Duration.ofMinutes(5))}
         * @return builder instance
         */
        public Builder candidateOffsetSetupStrategy(OffsetSetupStrategy strategy) {
            config.candidateOffsetSetupStrategy = strategy;
            return this;
        }

        public BGKafkaConsumerConfig build() {
            return config;
        }
    }

    @Deprecated(since = "9.0.10") // use another builder method
    public static Builder builder(Map<String, Object> properties, String topic, Supplier<String> m2mTokenSupplier,
                                  BlueGreenStatePublisher statePublisher) {
        return builder(properties, topic, statePublisher);
    }

    public static Builder builder(Map<String, Object> properties, String topic, BlueGreenStatePublisher statePublisher) {
        return builder(properties, Set.of(topic), statePublisher);
    }

    public static Builder builder(Map<String, Object> properties, Set<String> topics, BlueGreenStatePublisher statePublisher) {
        var groupId = Optional.ofNullable(properties.get(GROUP_ID_CONFIG))
                .map(Object::toString)
                .filter(s -> !s.isBlank())
                .orElseThrow(() -> new IllegalArgumentException("Consumer parameters should have `" + GROUP_ID_CONFIG +
                        "' key abd its value should be a non empty string"));

        BGKafkaConsumerConfig config = new BGKafkaConsumerConfig();
        config.properties = properties;
        config.topics = topics;
        config.groupIdPrefix = groupId;
        config.consumerSupplier = KafkaConsumer::new;
        config.adminAdapterSupplier = BGKafkaConsumerConfig::adminSupplier;
        config.statePublisher = statePublisher;
        return new Builder(config);
    }

    private static AdminAdapter adminSupplier(Map<String, Object> params) {
        var reduced = new HashMap<>(params);
        reduced.remove(GROUP_ID_CONFIG);
        reduced.remove("enable.auto.commit");
        return new AdminAdapterImpl(() -> Admin.create(reduced));
    }
}

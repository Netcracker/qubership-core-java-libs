package com.netcracker.cloud.maas.client.context.kafka;

import org.qubership.cloud.maas.client.api.Classifier;
import org.qubership.cloud.maas.client.api.kafka.KafkaMaaSClient;
import org.qubership.cloud.maas.client.api.kafka.TopicAddress;
import org.qubership.cloud.maas.client.api.kafka.TopicCreateOptions;
import org.qubership.cloud.maas.client.api.kafka.protocolextractors.OnTopicExists;
import lombok.Builder;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.*;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.TopicCollection;
import org.apache.kafka.common.Uuid;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException;
import org.apache.kafka.common.internals.KafkaFutureImpl;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Builder(builderMethodName = "")
public class MaaSKafkaAdminWrapper extends ForwardingAdmin {
    private final Map<String, Object> configs;
    private final KafkaMaaSClient kafkaMaaSClient;
    @Builder.Default
    private Function<String, Classifier> classifierSupplier = Classifier::new;
    @Builder.Default
    private OnTopicExists onTopicExistsAction = OnTopicExists.FAIL;

    private MaaSKafkaAdminWrapper(Map<String, Object> configs, KafkaMaaSClient kafkaMaaSClient,
                                  Function<String, Classifier> classifierSupplier,
                                  OnTopicExists onTopicExistsAction) {
        super(configs);
        this.configs = configs;
        this.kafkaMaaSClient = kafkaMaaSClient;
        this.classifierSupplier = classifierSupplier;
        this.onTopicExistsAction = onTopicExistsAction;
    }

    public static class MaaSKafkaAdminWrapperBuilder {
    }

    public static MaaSKafkaAdminWrapper.MaaSKafkaAdminWrapperBuilder builder(Map<String, Object> configs, KafkaMaaSClient kafkaMaaSClient) {
        return new MaaSKafkaAdminWrapperBuilder()
                .configs(configs)
                .kafkaMaaSClient(kafkaMaaSClient);
    }

    @Override
    public DescribeTopicsResult describeTopics(TopicCollection topics, DescribeTopicsOptions options) {
        DescribeTopicsResult describeTopicsResult = super.describeTopics(topics, options);
        // if topics is present in Kafka, check if it is present in MaaS as well
        if (topics instanceof TopicCollection.TopicNameCollection && describeTopicsResult.topicNameValues() != null) {
            Map<String, KafkaFuture<TopicDescription>> nameFutures = describeTopicsResult.topicNameValues().entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, e -> {
                        // if not present in MaaS - return KafkaFuture with UnknownTopicOrPartitionException result
                        String topicName = e.getKey();
                        Classifier classifier = getClassifier(topicName);
                        log.debug("Checking if topic '{}' is registered in MaaS by classifier '{}'", topicName, classifier);
                        if (this.kafkaMaaSClient.getTopic(classifier).isEmpty()) {
                            log.warn("Topic '{}' is not registered in MaaS by classifier '{}'. Return error future to force Admin to register topic in MaaS", topicName, classifier);
                            KafkaFutureImpl<TopicDescription> errorFuture = new KafkaFutureImpl<>();
                            errorFuture.completeExceptionally(new UnknownTopicOrPartitionException(String.format("Topic %s not found in MaaS by classifier %s", topicName, classifier)));
                            return errorFuture;
                        } else {
                            log.debug("Topic '{}' is registered in MaaS by classifier '{}'", topicName, classifier);
                            return e.getValue();
                        }
                    }));
            return new WrappedDescribeTopicsResult(null, nameFutures);
        } else {
            return describeTopicsResult;
        }
    }

    @Override
    public CreateTopicsResult createTopics(Collection<NewTopic> newTopics, CreateTopicsOptions options) {
        Map<String, KafkaFuture<CreateTopicsResult.TopicMetadataAndConfig>> wrappedFutures =
                newTopics.stream().collect(Collectors.toMap(
                        NewTopic::name,
                        newTopic -> {
                            String topicName = newTopic.name();
                            Classifier classifier = this.getClassifier(topicName);
                            TopicCreateOptions topicCreateOptions = getTopicCreateOptions(newTopic);
                            log.info("Performing getOrCreateTopic '{}' in MaaS by classifier '{}'", topicName, classifier);
                            TopicAddress topic = this.kafkaMaaSClient.getOrCreateTopic(classifier, topicCreateOptions);
                            int numPartitions = topic.getNumPartitions();
                            log.info("MaaS returned topic '{}' with numPartitions={}", topic.getTopicName(), numPartitions);
                            TopicDescription topicDescription = getTopicDescription(topicName);
                            Config config = getTopicConfig(topicName);
                            Uuid topicId = topicDescription.topicId();
                            int replicationFactor = 1;
                            return KafkaFuture.completedFuture(new CreateTopicsResult.TopicMetadataAndConfig(topicId, numPartitions, replicationFactor, config));
                        }));
        return new WrappedCreateTopicsResult(wrappedFutures);
    }

    protected Config getTopicConfig(String topicName) {
        ConfigResource configResource = new ConfigResource(ConfigResource.Type.TOPIC, topicName);
        List<ConfigResource> configResources = List.of(configResource);
        DescribeConfigsResult describeConfigsResult = this.describeConfigs(configResources);
        Map<ConfigResource, Config> topicConfigs = waitFuture(describeConfigsResult.all(), 1, TimeUnit.MINUTES);
        log.debug("Config for topic '{}' = {}", topicName, topicConfigs.get(configResource));
        return topicConfigs.get(configResource);
    }

    protected TopicDescription getTopicDescription(String topicName) {
        DescribeTopicsResult describeTopicsResult = this.describeTopics(List.of(topicName));
        Map<String, TopicDescription> topicDescriptions = waitFuture(describeTopicsResult.allTopicNames(), 1, TimeUnit.MINUTES);
        log.debug("TopicDescription for topic '{}' = {}", topicName, topicDescriptions.get(topicName));
        return topicDescriptions.get(topicName);
    }

    @SneakyThrows
    protected <T> T waitFuture(KafkaFuture<T> future, long timeout, TimeUnit unit) {
        return future.get(timeout, unit);
    }

    protected Classifier getClassifier(String topicName) {
        return classifierSupplier.apply(topicName);
    }

    protected TopicCreateOptions getTopicCreateOptions(NewTopic newTopic) {
        TopicCreateOptions topicCreateOptions = TopicCreateOptions.builder()
                .name(newTopic.name())
                .onTopicExists(this.onTopicExistsAction)
                .replicationFactor(newTopic.replicationFactor())
                .numPartitions(newTopic.numPartitions())
                .replicaAssignment(Optional.ofNullable(newTopic.replicasAssignments())
                        .map(assignments -> assignments.entrySet().stream().collect(Collectors.toMap(
                                e -> String.valueOf(e.getKey()), Map.Entry::getValue))).orElse(Map.of()))
                .configs(Optional.ofNullable(newTopic.configs()).orElse(Map.of()))
                .build();
        log.debug("TopicCreateOptions for NewTopic {} = {}", newTopic, topicCreateOptions);
        return topicCreateOptions;
    }

    private static class WrappedCreateTopicsResult extends CreateTopicsResult {
        protected WrappedCreateTopicsResult(Map<String, KafkaFuture<TopicMetadataAndConfig>> futures) {
            super(futures);
        }
    }

    private static class WrappedDescribeTopicsResult extends DescribeTopicsResult {
        protected WrappedDescribeTopicsResult(Map<Uuid, KafkaFuture<TopicDescription>> topicIdFutures, Map<String, KafkaFuture<TopicDescription>> nameFutures) {
            super(topicIdFutures, nameFutures);
        }
    }
}


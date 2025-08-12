package com.netcracker.cloud.maas.client.api.kafka;

import com.netcracker.cloud.maas.client.api.Classifier;
import com.netcracker.cloud.maas.client.api.MaaSException;
import com.netcracker.cloud.maas.client.impl.dto.kafka.v1.TopicTemplate;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public interface KafkaMaaSClient {
    /**
     * Get or create topic if it not exists.
     *
     * @param classifier classifier identifies topic
     * @param options topic create options like replication factor etc.
     * @return info about created topic
     */
    TopicAddress getOrCreateTopic(Classifier classifier, TopicCreateOptions options);

    /**
     * Generic method to get topic by explicitly specified classifier. It may be
     * useful in case of composite deployment and requests for topic owned by
     * different namespace then this microservice deployment
     * @param classifier custom classifier
     * @return {@link TopicAddress}
     */
    Optional<TopicAddress> getTopic(Classifier classifier);

    /**
     * Delete topic by given classifier. Result considered as successful if one or zero topics has been deleted.
     *
     * @param classifier part of classifier
     * @return true if topic found and successfully deleteed, false if topic not found
     * @throws MaaSException topic was found, but we encountered errors during deletion process
     */
    default boolean deleteTopic(Classifier classifier) {
        throw new UnsupportedOperationException("Should be implemented in subclasses");
    };

    /**
     * Add watch callback for tenant topics with `name'.
     *
     * <p>Classifier assembled for this request contains three fields:
     *  <ul>
     *      <li>name - name parameter value</li>
     *      <li>namespace - namespace value get from env variable NAMESPACE</li>
     *      <li>tenantId - wildcard: *</li>
     *  </ul>
     *
     * <p>Callback will be called at least once with current list of tenant topics and
     * each time when list of tenants is changed. List of tenant filtered by status and
     * considering only tenants in ACTIVE state.
     *
     * Typical code snippet to subscribe on tenant topics:
     * <pre>
     *     kafkaClient = new MaaSAPIClientImpl(() -&gt; token).getKafkaClient();
     *     kafkaClient.watchTenantTopics("orders", topics -&gt; {
     *             if (!topics.isEmpty()) {
     *                 KafkaConsumer&lt;Integer, OrderRecord&gt; consumer = getConsumer(topics.get(0).getBoostrapServers("PLAINTEXT"));
     *                 List&lt;String&gt; topicNames = topics.stream().map(t -&gt; t.getTopicName()).collect(Collectors.toList());
     *                 consumer.subscribe(topicNames);
     *             } else {
     *                 Optional.ofNullable(consumer).ifPresent(c -&gt; c.unsubscribe());
     *             }
     *         });
     * </pre>
     *
     * @param name name part of classifier for watchable topics
     * @param callback callback function executed each time when list of tenants changed
     */
    void watchTenantTopics(String name, Consumer<List<TopicAddress>> callback);

    /**
     * Wait for topic create event. Returns immediately if topic already exists.
     *
     * @param name name field of classifier for a topic we want to watch for
     * @param callback executed when topic will be created or immediately if topic is already exists
     * Requires MaaS server version 4.10.+
     */
    void watchTopicCreate(String name, Consumer<TopicAddress> callback);

    /**
     * Get or create topic using lazy-topic definition as template. Before use this method ensure that {@code nc.maas.kafka/lazy-topic} definition is already sent to maas
     * @param name name field value of lazy-topic definition
     * @return topic wrapper
     */
    TopicAddress getOrCreateLazyTopic(String name);

    /**
     * Get or create topic by lazy-topic definition by classifier:
     * <ul>
     *     <li>name</li>
     *     <li>namespace</li>
     *     <li>tenantId</li>
     * </ul>
     *
     * @param name name of lazy-topic definition
     * @param tenantId tenant id value
     * @return tenant address structure
     */
    TopicAddress getOrCreateLazyTopic(String name, String tenantId);

    /**
     * Get or create topic if it not exists.
     *
     * @param name topic template
     * @return TopicTemplate structure or error if there is topic linked to this topic template
     */
    TopicTemplate deleteTopicTemplate(String name);

    /**
     * Search for topics by given criteria
     */
    default List<TopicAddress> search(SearchCriteria criteria) {
        throw new UnsupportedOperationException("Should be implemented in subclasses");
    };
}

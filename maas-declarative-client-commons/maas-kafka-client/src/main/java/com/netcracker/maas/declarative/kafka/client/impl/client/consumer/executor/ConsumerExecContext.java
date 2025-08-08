package org.qubership.maas.declarative.kafka.client.impl.client.consumer.executor;

import org.qubership.cloud.maas.client.api.kafka.TopicAddress;
import org.qubership.maas.declarative.kafka.client.impl.client.consumer.DeserializerHolder;
import org.qubership.maas.declarative.kafka.client.impl.common.bg.KafkaConsumerConfiguration;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;

public class ConsumerExecContext {
    private TopicAddress topic;
    private Consumer handler;
    private Map<String, Object> connectionConfig;
    private ScheduledExecutorService executorService;
    private MaasConsumingExecutor executor;
    private DeserializerHolder deserializerHolder;
    private Duration pollDuration;
    private List<Long> awaitAfterErrorTimeList;
    private KafkaConsumerConfiguration kafkaConsumerConfiguration;

    public TopicAddress getTopic() {
        return topic;
    }

    public void setTopic(TopicAddress topic) {
        this.topic = topic;
    }

    public Consumer getHandler() {
        return handler;
    }

    public void setHandler(Consumer handler) {
        this.handler = handler;
    }

    public Map<String, Object> getConnectionConfig() {
        return connectionConfig;
    }

    public void setConnectionConfig(Map<String, Object> connectionConfig) {
        this.connectionConfig = connectionConfig;
    }

    public ScheduledExecutorService getExecutorService() {
        return executorService;
    }

    public void setExecutorService(ScheduledExecutorService executorService) {
        this.executorService = executorService;
    }

    public MaasConsumingExecutor getExecutor() {
        return executor;
    }

    public void setExecutor(MaasConsumingExecutor executor) {
        this.executor = executor;
    }

    public DeserializerHolder getDeserializerHolder() {
        return deserializerHolder;
    }

    public void setDeserializerHolder(DeserializerHolder deserializerHolder) {
        this.deserializerHolder = deserializerHolder;
    }

    public Duration getPollDuration() {
        return pollDuration;
    }

    public void setPollDuration(Duration pollDuration) {
        this.pollDuration = pollDuration;
    }

    public List<Long> getAwaitAfterErrorTimeList() {
        return awaitAfterErrorTimeList;
    }

    public void setAwaitAfterErrorTimeList(List<Long> awaitAfterErrorTimeList) {
        this.awaitAfterErrorTimeList = awaitAfterErrorTimeList;
    }

    public KafkaConsumerConfiguration getBlueGreenConfiguration() {
        return kafkaConsumerConfiguration;
    }

    public void setBlueGreenConfiguration(KafkaConsumerConfiguration kafkaConsumerConfiguration) {
        this.kafkaConsumerConfiguration = kafkaConsumerConfiguration;
    }
}

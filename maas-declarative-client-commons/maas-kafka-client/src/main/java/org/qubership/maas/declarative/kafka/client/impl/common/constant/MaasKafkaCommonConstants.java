package org.qubership.maas.declarative.kafka.client.impl.common.constant;

public interface MaasKafkaCommonConstants {
    // Thread wait value
    Long THREAD_WAIT = 5000L;
    // Used for reactivate initialized clients by ClientStateManagerService
    Long RETRY_ACTIVATION_TIMEOUT = 5000L;

    String CONSUMER_PREFIX = "maas.kafka.client.consumer.";
    String KAFKA_CONSUMER_PREFIX = "kafka-consumer.property.";

    String PRODUCER_PREFIX = "maas.kafka.client.producer.";
    String KAFKA_PRODUCER_PREFIX = "kafka-producer.property.";

    String ACCEPTABLE_TENANTS = "maas.kafka.acceptable-tenants";

    String CONSUMER_THREAD_POOL_SIZE = "maas.kafka.consumer-thread-pool-size";
    String CONSUMER_THREAD_POOL_SIZE_DEFAULT_VALUE = "2";

    String CONSUMER_POOL_DURATION = "maas.kafka.consumer-pool-duration";
    String CONSUMER_POOL_DURATION_DEFAULT_VALUE = "4000";

    String CONSUMER_AWAIT_TIMEOUT_AFTER_ERROR = "maas.kafka.consumer-await-timeout-after-error";

    String MAAS_AGENT_URL = "maas.agent.url";
    String MAAS_AGENT_URL_DEFAULT_VALUE = "http://maas-agent:8080";

    // Topic definition props
    String TOPIC_NAME = "topic.name";
    String TOPIC_ACTUAL_NAME = "topic.actual-name";
    String TOPIC_TEMPLATE = "topic.template";
    String TOPIC_NAMESPACE = "topic.namespace";
    String TOPIC_PARTITIONS = "topic.partitions";
    String TOPIC_REPLICATION_FACTOR = "topic.replication-factor";
    String TOPIC_MANAGED_BY = "topic.managedby";
    String TOPIC_CONFIGS = "topic.configs.";
    String TOPIC_ON_TOPIC_EXIST = "topic.on-topic-exists";
    String TOPIC_VERSIONED = "topic.versioned";


    // Client config props
    String IS_TENANT = "is-tenant";
    String GROUP_ID = "group.id";
    String INSTANCE_COUNT = "instance-count";
    String POOL_DURATION = "pool-duration";
    String DEDICATED_THREAD_POOL_SIZE = "dedicated-thread-pool-size";

    // Kafka localdev config
    String KAFKA_LOCAL_DEV_PREFIX = "maas.kafka.local-dev.";
    String KAFKA_LOCAL_DEV_ENABLED = "maas.kafka.local-dev.enabled";
    String KAFKA_LOCAL_DEV_CONFIG = "maas.kafka.local-dev.config.";
    String KAFKA_LOCAL_DEV_TENANTS_IDS = "maas.kafka.local-dev.tenant-ids";
    String KAFKA_CONSUMER_BLUE_GREEN_PREFIX = "blue-green.";
}

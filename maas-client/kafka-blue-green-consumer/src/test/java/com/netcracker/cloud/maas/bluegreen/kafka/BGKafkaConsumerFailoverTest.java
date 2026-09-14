package com.netcracker.cloud.maas.bluegreen.kafka;

import com.netcracker.cloud.bluegreen.impl.service.InMemoryBlueGreenStatePublisher;
import com.netcracker.cloud.maas.bluegreen.kafka.impl.BGKafkaConsumerConfig;
import com.netcracker.cloud.maas.bluegreen.kafka.impl.BGKafkaConsumerImpl;
import com.netcracker.cloud.maas.bluegreen.kafka.util.KafkaContainerCluster;
import com.netcracker.cloud.maas.client.impl.Env;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static org.apache.kafka.clients.CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * The broker leading the partition goes while a consumer is reading. The same consumer
 * instance has to read and commit the rest of the topic.
 */
@Slf4j
class BGKafkaConsumerFailoverTest {

    private static final String NAMESPACE = "test-ns-1";
    private static final String TOPIC_NAME = "orders";
    private static final int BROKERS = 3;
    private static final int REPLICATION_FACTOR = 2;
    private static final int RECORDS_BEFORE_LOSS = 5;
    private static final int RECORDS_AFTER_LOSS = 5;
    // the group has to find its coordinator again, which is most of the wait
    private static final Duration RECOVERY_ALLOWANCE = Duration.ofSeconds(60);
    private static final Duration POLL_TIMEOUT = Duration.ofSeconds(5);

    private static final Supplier<String> M2M_TOKEN_SUPPLIER = () -> "fake";

    /**
     * How a broker leaves. A node replacement drains it, so it hands its partitions over
     * before it exits; a node that dies does not. Both are real events.
     */
    enum BrokerFault {
        DRAINED {
            @Override
            void apply(KafkaContainerCluster cluster, int broker) {
                cluster.drainBroker(broker);
            }
        },
        KILLED {
            @Override
            void apply(KafkaContainerCluster cluster, int broker) {
                cluster.killBroker(broker);
            }
        };

        abstract void apply(KafkaContainerCluster cluster, int broker);
    }

    private KafkaContainerCluster cluster;
    private Admin admin;
    private String bootstrapServers;
    private Properties producerProps;

    // a cluster per test: the two faults leave it in different states, and a killed
    // broker does not come back quickly enough to be reused
    @BeforeEach
    void setupKafka() {
        System.setProperty(Env.PROP_NAMESPACE, NAMESPACE);
        cluster = new KafkaContainerCluster("7.4.0", BROKERS, REPLICATION_FACTOR, false);
        cluster.start();
        bootstrapServers = cluster.getBootstrapServers();
        log.info("bootstrap servers: {}", bootstrapServers);

        Properties adminProps = new Properties();
        adminProps.put(BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        admin = Admin.create(adminProps);

        producerProps = new Properties();
        producerProps.putAll(Map.of(
                ProducerConfig.CLIENT_ID_CONFIG, "failover-producer",
                BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
                // acks=all, so an acknowledged record is on both replicas and must
                // survive the loss of either
                ProducerConfig.ACKS_CONFIG, "all",
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName(),
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName()));
    }

    @AfterEach
    void stopKafka() {
        Optional.ofNullable(admin).ifPresent(Admin::close);
        Optional.ofNullable(cluster).ifPresent(KafkaContainerCluster::stop);
    }

    @ParameterizedTest
    @EnumSource(BrokerFault.class)
    void testConsumerSurvivesPartitionLeaderLoss(BrokerFault fault) throws Exception {
        String topicName = TOPIC_NAME + "-" + fault.name().toLowerCase();
        admin.createTopics(List.of(new NewTopic(topicName, 1, (short) REPLICATION_FACTOR))).all().get();

        var connectionProperties = Map.<String, Object>of(
                BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
                "group.id", "failover-" + fault.name().toLowerCase(),
                "enable.auto.commit", "false",
                // one batch per poll, so the second half is still unread when the
                // leader goes away
                "max.poll.records", String.valueOf(RECORDS_BEFORE_LOSS),
                // a blocked call gives up inside the allowance; request.timeout.ms keeps
                // its default, a shorter one cuts JoinGroup and the group never forms
                "default.api.timeout.ms", "30000",
                // a fetch too small to hold the topic, so the second half stays on the
                // broker rather than in the client buffer
                "max.partition.fetch.bytes", "256");

        // produced while the cluster is whole, so it is the consumer that has to find
        // the new leader, not the producer
        produce(topicName, 0, RECORDS_BEFORE_LOSS + RECORDS_AFTER_LOSS);

        try (BGKafkaConsumerImpl<String, String> consumer = new BGKafkaConsumerImpl<>(
                BGKafkaConsumerConfig.builder(connectionProperties, topicName, M2M_TOKEN_SUPPLIER,
                                new InMemoryBlueGreenStatePublisher(Env.namespace()))
                        .deserializers(new StringDeserializer(), new StringDeserializer())
                        .consistencyMode(ConsumerConsistencyMode.GUARANTEE_CONSUMPTION)
                        .build())) {

            Set<String> seen = new HashSet<>();
            // a batch can overshoot the target, so this is a lower bound
            drainInto(consumer, seen, RECORDS_BEFORE_LOSS);
            assertTrue(seen.size() >= RECORDS_BEFORE_LOSS,
                    "part of the topic must be read before the leader goes, got " + seen.size());

            int leader = partitionLeader(topicName);
            log.info("{} broker {}, which leads partition 0 of {}", fault, leader, topicName);
            // from the fault, because where the time lands between the cluster settling
            // and the consumer reconnecting depends on what happens in between
            long start = System.nanoTime();
            fault.apply(cluster, leader);

            // delivery is at least once: a record whose commit did not land is read
            // again, so the measure is the set of keys, not the count of records
            drainInto(consumer, seen, RECORDS_BEFORE_LOSS + RECORDS_AFTER_LOSS);
            log.info("leader {}: the consumer read and committed the rest {}ms after the fault",
                    fault, TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start));
            assertEquals(RECORDS_BEFORE_LOSS + RECORDS_AFTER_LOSS, seen.size(),
                    "losing the partition leader must not lose a record");
        }
    }

    /** Reads and commits until the expected number of distinct keys has arrived. */
    private void drainInto(BGKafkaConsumerImpl<String, String> consumer, Set<String> seen, int expected) {
        long deadline = System.nanoTime() + RECOVERY_ALLOWANCE.toNanos();
        Exception lastError = null;
        while (seen.size() < expected && System.nanoTime() < deadline) {
            try {
                Optional<RecordsBatch<String, String>> batch = consumer.poll(POLL_TIMEOUT);
                if (batch.isEmpty()) {
                    continue;
                }
                batch.get().getBatch().forEach(record -> seen.add(record.getConsumerRecord().key()));
                consumer.commitSync(batch.get().getCommitMarker());
            } catch (Exception e) {
                // the errors a broker loss produces are transient: the client has to find
                // the new leader, and the group its new coordinator
                lastError = e;
                log.info("retrying after: {}", e.getMessage());
            }
        }
        if (seen.size() < expected) {
            fail("only " + seen.size() + " of " + expected + " records arrived within " + RECOVERY_ALLOWANCE
                    + (lastError == null ? "" : ", last error: " + lastError));
        }
    }

    private int partitionLeader(String topicName) throws Exception {
        return admin.describeTopics(List.of(topicName)).allTopicNames().get()
                .get(topicName).partitions().get(0).leader().id();
    }

    private void produce(String topicName, int from, int count) {
        try (KafkaProducer<String, String> producer = new KafkaProducer<>(producerProps)) {
            for (int i = from; i < from + count; i++) {
                String key = String.format("%04d", i);
                try {
                    producer.send(new ProducerRecord<>(topicName, 0, key, "order-" + key))
                            .get(30, TimeUnit.SECONDS);
                } catch (Exception e) {
                    throw new IllegalStateException("failed to produce " + key, e);
                }
            }
        }
    }
}

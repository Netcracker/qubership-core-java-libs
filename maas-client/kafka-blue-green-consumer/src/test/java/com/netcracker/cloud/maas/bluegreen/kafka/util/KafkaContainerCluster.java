package org.qubership.cloud.maas.bluegreen.kafka.util;

import org.apache.kafka.common.Uuid;
import org.rnorth.ducttape.unreliables.Unreliables;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy;
import org.testcontainers.lifecycle.Startable;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class KafkaContainerCluster implements Startable {

    private final int brokersNum;

    private final Network network;
    private GenericContainer zookeeper;
    private final Collection<KafkaContainer> brokers;

    public KafkaContainerCluster(String confluentPlatformVersion, int brokersNum, int replicationFactor, boolean useZookeeper) {
        if (brokersNum < 0) {
            throw new IllegalArgumentException("brokersNum '" + brokersNum + "' must be greater than 0");
        }
        if (replicationFactor < 0 || replicationFactor > brokersNum) {
            throw new IllegalArgumentException(
                    "replicationFactor '" + replicationFactor + "' must be less than brokersNum and greater than 0"
            );
        }

        this.brokersNum = brokersNum;
        this.network = Network.newNetwork();

        String controllerQuorumVoters = IntStream.range(0, brokersNum)
                .mapToObj(brokerNum -> String.format("%d@broker-%d:9094", brokerNum, brokerNum))
                .collect(Collectors.joining(","));

        String clusterId = Uuid.randomUuid().toString();
        if (useZookeeper) {
            zookeeper = new GenericContainer<>("confluentinc/cp-zookeeper:4.0.0").withEnv("ZOOKEEPER_CLIENT_PORT", "2181")
                    .withExposedPorts(2181)
                    .withNetwork(this.network)
                    .withNetworkAliases("zookeeper")
                    .waitingFor(new HostPortWaitStrategy().forPorts(2181));
            zookeeper.start();
        }
        this.brokers = IntStream.range(0, brokersNum).mapToObj(brokerNum -> {
                    KafkaContainer kafkaContainer = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka").withTag(confluentPlatformVersion))
                            .withNetwork(this.network)
                            .withNetworkAliases("broker-" + brokerNum)
                            .withClusterId(clusterId)
                            .withEnv("KAFKA_BROKER_ID", brokerNum + "")
                            .withEnv("KAFKA_NODE_ID", brokerNum + "")
                            .withEnv("KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR", replicationFactor + "")
                            .withEnv("KAFKA_OFFSETS_TOPIC_NUM_PARTITIONS", replicationFactor + "")
                            .withEnv("KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR", replicationFactor + "")
                            .withEnv("KAFKA_TRANSACTION_STATE_LOG_MIN_ISR", replicationFactor + "")
                            .withStartupTimeout(Duration.ofMinutes(1));
                    if (useZookeeper) {
                        kafkaContainer.withExternalZookeeper("zookeeper:2181");
                    } else {
                        kafkaContainer.withEnv("KAFKA_CONTROLLER_QUORUM_VOTERS", controllerQuorumVoters);
                        kafkaContainer.withKraft();
                    }
                    return kafkaContainer;
                })
                .collect(Collectors.toList());
    }

    public Collection<KafkaContainer> getBrokers() {
        return this.brokers;
    }

    public String getBootstrapServers() {
        return brokers.stream().map(KafkaContainer::getBootstrapServers).collect(Collectors.joining(","));
    }

    @Override
    public void start() {
        // Needs to start all the brokers at once
        brokers.parallelStream().forEach(GenericContainer::start);
        if (zookeeper != null) {
            Unreliables.retryUntilTrue(30, TimeUnit.SECONDS, () -> {
                Container.ExecResult result = this.zookeeper.execInContainer(
                        "sh", "-c", "zookeeper-shell zookeeper:" + KafkaContainer.ZOOKEEPER_PORT + " ls /brokers/ids | tail -n 1");
                String brokers = result.getStdout();
                return brokers != null && brokers.split(",").length == this.brokersNum;
            });
        } else {
            Unreliables.retryUntilTrue(30, TimeUnit.SECONDS, () -> {
                        Container.ExecResult result = this.brokers.stream().findFirst().get().execInContainer("sh", "-c",
                                "kafka-metadata-shell --snapshot /var/lib/kafka/data/__cluster_metadata-0/00000000000000000000.log ls /brokers | wc -l");
                        String brokers = result.getStdout().replace("\n", "");
                        return Integer.valueOf(brokers) == this.brokersNum;
                    }
            );
        }
    }

    @Override
    public void stop() {
        this.brokers.stream().parallel().forEach(GenericContainer::stop);
        Optional.ofNullable(zookeeper).ifPresent(GenericContainer::stop);
    }
}
package com.netcracker.cloud.maas.bluegreen.kafka.util;

import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.Ports;
import org.apache.kafka.common.Uuid;
import org.rnorth.ducttape.unreliables.Unreliables;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy;
import org.testcontainers.lifecycle.Startable;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class KafkaContainerCluster implements Startable {

    private final int brokersNum;

    private final Network network;
    private GenericContainer zookeeper;
    private final List<KafkaContainer> brokers;

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
        // bound at creation, so a broker that is stopped and started again comes back at
        // the address clients already know
        List<Integer> hostPorts = reservePorts(brokersNum);

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
                    int hostPort = hostPorts.get(brokerNum);
                    KafkaContainer kafkaContainer = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka").withTag(confluentPlatformVersion))
                            .withNetwork(this.network)
                            .withNetworkAliases("broker-" + brokerNum)
                            .withClusterId(clusterId)
                            .withEnv("KAFKA_BROKER_ID", brokerNum + "")
                            .withEnv("KAFKA_NODE_ID", brokerNum + "")
                            .withEnv("KAFKA_GROUP_INITIAL_REBALANCE_DELAY_MS", "0")
                            .withEnv("KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR", replicationFactor + "")
                            .withEnv("KAFKA_OFFSETS_TOPIC_NUM_PARTITIONS", replicationFactor + "")
                            .withEnv("KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR", replicationFactor + "")
                            .withEnv("KAFKA_TRANSACTION_STATE_LOG_MIN_ISR", replicationFactor + "")
                            .withStartupTimeout(Duration.ofMinutes(1));
                    kafkaContainer.withCreateContainerCmdModifier(cmd -> Optional.ofNullable(cmd.getHostConfig())
                            .ifPresent(hostConfig -> {
                                Ports bindings = Optional.ofNullable(hostConfig.getPortBindings()).orElseGet(Ports::new);
                                bindings.bind(ExposedPort.tcp(KafkaContainer.KAFKA_PORT), Ports.Binding.bindPort(hostPort));
                                hostConfig.withPortBindings(bindings);
                            }));
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

    /** The broker with the given id: ids match positions, KAFKA_BROKER_ID is the position. */
    public KafkaContainer getBroker(int brokerId) {
        return this.brokers.get(brokerId);
    }

    public String getBootstrapServers() {
        return brokers.stream().map(KafkaContainer::getBootstrapServers).collect(Collectors.joining(","));
    }

    /**
     * Kills one broker, leaving the container and its data in place so it can be started
     * again. The stop signal reaches a wrapper shell rather than the broker itself, so
     * there is no controlled shutdown to wait for and the timeout is zero.
     */
    public void killBroker(int brokerId) {
        KafkaContainer broker = getBroker(brokerId);
        broker.getDockerClient().stopContainerCmd(broker.getContainerId()).withTimeout(0).exec();
    }

    /** Starts a stopped broker back at the address and with the data it had. */
    public void startBroker(int brokerId) {
        KafkaContainer broker = getBroker(brokerId);
        if (!isRunning(broker)) {
            broker.getDockerClient().startContainerCmd(broker.getContainerId()).exec();
        }
        awaitBrokersRegistered();
    }

    private boolean isRunning(KafkaContainer broker) {
        return Boolean.TRUE.equals(broker.getDockerClient()
                .inspectContainerCmd(broker.getContainerId()).exec().getState().getRunning());
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
            awaitBrokersRegistered();
        }
    }

    @Override
    public void stop() {
        this.brokers.stream().parallel().forEach(GenericContainer::stop);
        Optional.ofNullable(zookeeper).ifPresent(GenericContainer::stop);
    }

    /** Waits until the cluster metadata lists every broker. */
    private void awaitBrokersRegistered() {
        Unreliables.retryUntilTrue(60, TimeUnit.SECONDS, () -> {
                    KafkaContainer running = this.brokers.stream()
                            .filter(GenericContainer::isRunning)
                            .findFirst()
                            .orElseThrow(() -> new IllegalStateException("no broker is running"));
                    Container.ExecResult result = running.execInContainer("sh", "-c",
                            "kafka-metadata-shell --snapshot /var/lib/kafka/data/__cluster_metadata-0/00000000000000000000.log ls /brokers | wc -l");
                    String brokers = result.getStdout().replace("\n", "");
                    return Integer.valueOf(brokers) == this.brokersNum;
                }
        );
    }

    /**
     * Picks one free port per broker, holding every socket open until all of them are
     * chosen so that two brokers cannot be handed the same port.
     */
    private static List<Integer> reservePorts(int count) {
        List<ServerSocket> sockets = new ArrayList<>(count);
        try {
            List<Integer> ports = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                ServerSocket socket = new ServerSocket(0);
                sockets.add(socket);
                ports.add(socket.getLocalPort());
            }
            return ports;
        } catch (IOException e) {
            throw new UncheckedIOException("failed to reserve host ports for the brokers", e);
        } finally {
            sockets.forEach(socket -> {
                try {
                    socket.close();
                } catch (IOException ignored) {
                    // closing is what leaves the port free for the container to bind
                }
            });
        }
    }
}

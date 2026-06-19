package com.netcracker.cloud.maas.bluegreen.kafka.impl;

import com.netcracker.cloud.bluegreen.impl.service.InMemoryBlueGreenStatePublisher;
import com.netcracker.cloud.maas.bluegreen.kafka.PartitionsAssignedListener;
import com.netcracker.cloud.maas.client.impl.Env;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultKafkaConsumerPartitionsAssignedListenerTest {

  @Test
  void partitionsAssignedListenerIsInvokedOnRebalance() {
    System.setProperty(Env.PROP_NAMESPACE, "test-namespace");
    Consumer<String, String> kafkaConsumer = mock(Consumer.class);
    ConsumerRebalanceListener[] rebalanceListener = new ConsumerRebalanceListener[1];
    doAnswer(inv -> {
      rebalanceListener[0] = inv.getArgument(1);
      return null;
    }).when(kafkaConsumer).subscribe(any(Collection.class), any(ConsumerRebalanceListener.class));
    Map<TopicPartition, OffsetAndMetadata> committedOffsets = new HashMap<>();
    committedOffsets.put(new TopicPartition("orders", 0), null);
    when(kafkaConsumer.committed(anySet())).thenReturn(committedOffsets);

    var config = BGKafkaConsumerConfig.builder(
            Map.of("group.id", "g1", "bootstrap.servers", "localhost:9092"),
            "orders",
            () -> "token",
            new InMemoryBlueGreenStatePublisher(Env.namespace()))
        .consumerSupplier(props -> kafkaConsumer)
        .build();

    var invoked = new AtomicBoolean();
    try (var consumer = new DefaultKafkaConsumer<String, String>(config)) {
      consumer.setPartitionsAssignedListener((PartitionsAssignedListener) partitions -> invoked.set(true));
      var partitions = List.of(new TopicPartition("orders", 0));
      rebalanceListener[0].onPartitionsAssigned(partitions);
    }

    assertTrue(invoked.get());
    verify(kafkaConsumer).seek(new TopicPartition("orders", 0), 0L);
  }

  @Test
  void partitionsAssignedListenerUsesCommittedOffsetWhenPresent() {
    System.setProperty(Env.PROP_NAMESPACE, "test-namespace");
    Consumer<String, String> kafkaConsumer = mock(Consumer.class);
    ConsumerRebalanceListener[] rebalanceListener = new ConsumerRebalanceListener[1];
    doAnswer(inv -> {
      rebalanceListener[0] = inv.getArgument(1);
      return null;
    }).when(kafkaConsumer).subscribe(any(Collection.class), any(ConsumerRebalanceListener.class));

    var tp = new TopicPartition("orders", 1);
    when(kafkaConsumer.committed(anySet())).thenReturn(Map.of(tp, new OffsetAndMetadata(42L)));

    var config = BGKafkaConsumerConfig.builder(
            Map.of("group.id", "g1", "bootstrap.servers", "localhost:9092"),
            "orders",
            () -> "token",
            new InMemoryBlueGreenStatePublisher(Env.namespace()))
        .consumerSupplier(props -> kafkaConsumer)
        .build();

    try (var consumer = new DefaultKafkaConsumer<String, String>(config)) {
      consumer.setPartitionsAssignedListener(partitions -> {});
      rebalanceListener[0].onPartitionsAssigned(List.of(tp));
    }

    verify(kafkaConsumer).seek(tp, 42L);
  }
}

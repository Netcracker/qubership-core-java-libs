package com.netcracker.cloud.maas.client.impl.dto.kafka.v1;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class TopicSettingsRequest {
    int numPartitions;
    int minNumPartitions;
    String replicationFactor;
    Map<String, List<Integer>> replicaAssignment;
    Map<String, String> configs;
}

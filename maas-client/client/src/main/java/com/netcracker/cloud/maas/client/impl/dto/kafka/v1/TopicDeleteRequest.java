package com.netcracker.cloud.maas.client.impl.dto.kafka.v1;

import com.netcracker.cloud.maas.client.api.Classifier;
import lombok.Data;

@Data
public class TopicDeleteRequest {
    final Classifier classifier;
}

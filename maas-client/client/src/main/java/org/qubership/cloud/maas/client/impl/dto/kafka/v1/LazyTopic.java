package org.qubership.cloud.maas.client.impl.dto.kafka.v1;

import org.qubership.cloud.maas.client.api.Classifier;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@EqualsAndHashCode(callSuper = true)
public class LazyTopic extends TopicSettings {
    Classifier classifier;
}

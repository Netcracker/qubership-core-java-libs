package org.qubership.cloud.maas.client.impl.dto.kafka.v1;

import lombok.*;

@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class TopicTemplate extends TopicSettingsRequest {
    String name;
    String namespace;
}

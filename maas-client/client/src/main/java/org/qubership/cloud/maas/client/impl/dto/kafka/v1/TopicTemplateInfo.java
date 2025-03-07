package org.qubership.cloud.maas.client.impl.dto.kafka.v1;

import lombok.Data;
import lombok.Generated;

import java.util.List;

/**
 * Response on topic-template get/create request
 */
@Data
@Generated
public class TopicTemplateInfo {
    private String name;
    private String namespace;
    private TopicSettings currentSettings;
    private TopicSettings previousSettings;
    private List<TopicInfo> updatedTopics;
}

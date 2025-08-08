package com.netcracker.cloud.maas.client.impl.dto.kafka.v1;

import org.qubership.cloud.maas.client.api.Classifier;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Response on topic get/create request
 */
@Data
public class TopicInfo {
    private Map<String, List<String>> addresses;
    private String name;
    private Classifier classifier;
    private String namespace;
    private boolean externallyManaged;
    private String instance;
    private String caCert;

    private Map<String, List<TopicUserCredentialsImpl>> credential;
    private TopicSettingsRequest requestedSettings;
    private TopicSettings actualSettings;
    private String template;
    private boolean versioned;
}

package org.qubership.maas.declarative.kafka.client.impl.common.cred.extractor.impl;

import org.qubership.cloud.maas.client.api.kafka.TopicAddress;
import org.qubership.maas.declarative.kafka.client.impl.common.cred.extractor.api.InternalMaasTopicCredentialsExtractor;

import java.util.Map;

// Localdev only
public class LocalDevInternalTopicCredentialsExtractorImpl implements InternalMaasTopicCredentialsExtractor {

    private final Map<String, Object> kafkaCreds;

    public LocalDevInternalTopicCredentialsExtractorImpl(Map<String, Object> kafkaCreds) {
        this.kafkaCreds = kafkaCreds;
    }

    @Override
    public Map<String, Object> extract(TopicAddress topic) {
        return kafkaCreds;
    }
}

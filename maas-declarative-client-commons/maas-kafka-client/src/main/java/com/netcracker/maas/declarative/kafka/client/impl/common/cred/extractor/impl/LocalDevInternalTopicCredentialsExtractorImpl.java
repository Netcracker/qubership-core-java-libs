package com.netcracker.maas.declarative.kafka.client.impl.common.cred.extractor.impl;

import com.netcracker.cloud.maas.client.api.kafka.TopicAddress;
import com.netcracker.maas.declarative.kafka.client.impl.common.cred.extractor.api.InternalMaasTopicCredentialsExtractor;

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

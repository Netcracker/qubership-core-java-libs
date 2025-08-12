package com.netcracker.maas.declarative.kafka.client.impl.common.cred.extractor.api;

import com.netcracker.cloud.maas.client.api.kafka.TopicAddress;

import java.util.Map;

// TODO added temporary, will be used default platform cred extractor
public interface InternalMaasTopicCredentialsExtractor {

    Map<String, Object> extract(TopicAddress topic);

}

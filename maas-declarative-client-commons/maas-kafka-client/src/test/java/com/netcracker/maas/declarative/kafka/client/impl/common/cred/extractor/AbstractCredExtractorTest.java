package com.netcracker.maas.declarative.kafka.client.impl.common.cred.extractor;

import org.qubership.maas.declarative.kafka.client.api.MaasKafkaTopicService;
import org.qubership.maas.declarative.kafka.client.impl.common.cred.extractor.api.InternalMaasTopicCredentialsExtractor;
import org.qubership.maas.declarative.kafka.client.impl.common.cred.extractor.impl.DefaultInternalMaasTopicCredentialsExtractorImpl;

public abstract class AbstractCredExtractorTest {

    // Extracts topics from resource folder
    protected final MaasKafkaTopicService maasKafkaTopicService = new MaasKafkaTopicServiceTestImpl();

    protected final InternalMaasTopicCredentialsExtractor credentialsExtractor = new DefaultInternalMaasTopicCredentialsExtractorImpl();

}

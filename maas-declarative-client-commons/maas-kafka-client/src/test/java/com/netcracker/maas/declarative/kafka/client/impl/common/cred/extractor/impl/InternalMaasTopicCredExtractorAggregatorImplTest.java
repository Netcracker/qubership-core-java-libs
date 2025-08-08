package com.netcracker.maas.declarative.kafka.client.impl.common.cred.extractor.impl;

import org.qubership.cloud.maas.client.impl.dto.kafka.v1.TopicInfo;
import org.qubership.cloud.maas.client.impl.dto.kafka.v1.TopicUserCredentialsImpl;
import org.qubership.cloud.maas.client.impl.kafka.TopicAddressImpl;
import org.qubership.maas.declarative.kafka.client.impl.common.cred.extractor.provider.impl.DefaultInternalMaasTopicCredentialsExtractorProviderImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class InternalMaasTopicCredExtractorAggregatorImplTest {

    InternalMaasTopicCredExtractorAggregatorImpl extractor;

    @BeforeEach
    void setUp() {
        DefaultInternalMaasTopicCredentialsExtractorImpl defaultInternalMaasTopicCredentialsExtractor = new DefaultInternalMaasTopicCredentialsExtractorImpl();
        DefaultInternalMaasTopicCredentialsExtractorProviderImpl defaultInternalMaasTopicCredentialsExtractorProvider =
                new DefaultInternalMaasTopicCredentialsExtractorProviderImpl(defaultInternalMaasTopicCredentialsExtractor);
        extractor = new InternalMaasTopicCredExtractorAggregatorImpl(Collections.singletonList(defaultInternalMaasTopicCredentialsExtractorProvider));
    }

    @Test
    void extract() {
        TopicInfo topicInfo = new TopicInfo();
        topicInfo.setName("test-topic-name");
        topicInfo.setAddresses(Map.of("SASL_SSL", List.of("localhost:9092")));
        TopicUserCredentialsImpl topicUserCredentials = new TopicUserCredentialsImpl();
        topicUserCredentials.setType("SCRAM");
        topicUserCredentials.setUsername("test-user");
        topicUserCredentials.setEncodedPassword("plain:encoded-password");
        topicInfo.setCredential(Map.of("client", Collections.singletonList(topicUserCredentials)));
        topicInfo.setCaCert("my-test-ca-cert");
        TopicAddressImpl topicAddress = new TopicAddressImpl(topicInfo);
        Map<String, Object> extracted = extractor.extract(topicAddress);

        assertNotNull(extracted);
        assertFalse(extracted.isEmpty());
        assertEquals("SASL_SSL", extracted.get("security.protocol"));
        assertEquals("localhost:9092",  extracted.get("bootstrap.servers"));
        assertEquals("-----BEGIN CERTIFICATE----- my-test-ca-cert -----END CERTIFICATE-----", extracted.get("ssl.truststore.certificates"));
        assertEquals("org.apache.kafka.common.security.scram.ScramLoginModule required username=\"test-user\" password=\"encoded-password\";", extracted.get("sasl.jaas.config"));
    }
}

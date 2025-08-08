package com.netcracker.maas.declarative.kafka.client.impl.common.cred.extractor.impl;

import org.qubership.cloud.maas.client.api.kafka.TopicAddress;
import org.qubership.maas.declarative.kafka.client.api.exception.MaasKafkaException;
import org.qubership.maas.declarative.kafka.client.impl.common.cred.extractor.provider.api.InternalMaasCredExtractorProvider;
import org.qubership.maas.declarative.kafka.client.impl.common.cred.extractor.api.InternalMaasTopicCredentialsExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// Main extractor
public class InternalMaasTopicCredExtractorAggregatorImpl implements InternalMaasTopicCredentialsExtractor {

    private static final Logger LOG = LoggerFactory.getLogger(InternalMaasTopicCredExtractorAggregatorImpl.class);

    private final List<InternalMaasCredExtractorProvider> extractorProviders;

    public InternalMaasTopicCredExtractorAggregatorImpl(List<InternalMaasCredExtractorProvider> extractorProviders) {
        this.extractorProviders = sortExtractors(extractorProviders);
    }


    /*
        _________________________________________________
        | Order of cred extractors:
        -------------------------------------------------
        | 0) LocalDevInternalTopicCredentialsExtractor (LocalDev only)
        -------------------------------------------------
        | 1) DefaultInternalMaasTopicCredentialsExtractor
        -------------------------------------------------
    * */
    @Override
    public Map<String, Object> extract(TopicAddress topic) {
        for (InternalMaasCredExtractorProvider provider : extractorProviders) {
            try {
                if (provider.provide() != null) {
                    Map<String, Object> creds = provider.provide().extract(topic);
                    if (creds != null) {
                        return creds;
                    }
                }
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
            }
        }
        throw new MaasKafkaException("No valid provider for cred extractor");
    }

    private List<InternalMaasCredExtractorProvider> sortExtractors(List<InternalMaasCredExtractorProvider> extractors) {
        return extractors.stream().sorted(Comparator.comparing(InternalMaasCredExtractorProvider::order)).collect(Collectors.toList());
    }
}

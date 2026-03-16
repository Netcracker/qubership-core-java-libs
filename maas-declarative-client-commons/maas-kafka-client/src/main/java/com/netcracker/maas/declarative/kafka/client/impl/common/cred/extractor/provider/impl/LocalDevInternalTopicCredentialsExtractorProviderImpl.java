package com.netcracker.maas.declarative.kafka.client.impl.common.cred.extractor.provider.impl;

import com.netcracker.maas.declarative.kafka.client.impl.common.cred.extractor.api.InternalMaasTopicCredentialsExtractor;
import com.netcracker.maas.declarative.kafka.client.impl.common.cred.extractor.impl.LocalDevInternalTopicCredentialsExtractorImpl;
import com.netcracker.maas.declarative.kafka.client.impl.common.cred.extractor.provider.api.InternalMaasCredExtractorProvider;

// Lcoaldev only
public class LocalDevInternalTopicCredentialsExtractorProviderImpl implements InternalMaasCredExtractorProvider {

    private final LocalDevInternalTopicCredentialsExtractorImpl credentialsExtractor;

    public LocalDevInternalTopicCredentialsExtractorProviderImpl(LocalDevInternalTopicCredentialsExtractorImpl credentialsExtractor) {
        this.credentialsExtractor = credentialsExtractor;
    }

    @Override
    public InternalMaasTopicCredentialsExtractor provide() {
        return credentialsExtractor;
    }

    @Override
    public int order() {
        // To override default extractor
        return InternalMaasCredExtractorProvider.super.order() - 1;
    }
}

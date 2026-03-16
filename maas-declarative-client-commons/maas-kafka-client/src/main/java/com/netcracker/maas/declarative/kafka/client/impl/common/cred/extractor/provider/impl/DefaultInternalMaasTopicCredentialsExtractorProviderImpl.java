package com.netcracker.maas.declarative.kafka.client.impl.common.cred.extractor.provider.impl;

import com.netcracker.maas.declarative.kafka.client.impl.common.cred.extractor.api.InternalMaasTopicCredentialsExtractor;
import com.netcracker.maas.declarative.kafka.client.impl.common.cred.extractor.impl.DefaultInternalMaasTopicCredentialsExtractorImpl;
import com.netcracker.maas.declarative.kafka.client.impl.common.cred.extractor.provider.api.InternalMaasCredExtractorProvider;

public class DefaultInternalMaasTopicCredentialsExtractorProviderImpl implements InternalMaasCredExtractorProvider {

    private final DefaultInternalMaasTopicCredentialsExtractorImpl credentialsExtractor;

    public DefaultInternalMaasTopicCredentialsExtractorProviderImpl(DefaultInternalMaasTopicCredentialsExtractorImpl credentialsExtractor) {
        this.credentialsExtractor = credentialsExtractor;
    }

    @Override
    public InternalMaasTopicCredentialsExtractor provide() {
        return credentialsExtractor;
    }
}

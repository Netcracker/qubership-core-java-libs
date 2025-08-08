package org.qubership.maas.declarative.kafka.client.impl.common.cred.extractor.provider.impl;

import org.qubership.maas.declarative.kafka.client.impl.common.cred.extractor.api.InternalMaasTopicCredentialsExtractor;
import org.qubership.maas.declarative.kafka.client.impl.common.cred.extractor.impl.DefaultInternalMaasTopicCredentialsExtractorImpl;
import org.qubership.maas.declarative.kafka.client.impl.common.cred.extractor.provider.api.InternalMaasCredExtractorProvider;

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

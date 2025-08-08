package org.qubership.maas.declarative.kafka.client.impl.common.cred.extractor.provider.api;


import org.qubership.maas.declarative.kafka.client.impl.common.cred.extractor.api.InternalMaasTopicCredentialsExtractor;

// TODO will be used to provide any type of providers
public interface InternalMaasCredExtractorProvider {

    InternalMaasTopicCredentialsExtractor provide();

    default int order() {
        return Integer.MAX_VALUE;
    }
}

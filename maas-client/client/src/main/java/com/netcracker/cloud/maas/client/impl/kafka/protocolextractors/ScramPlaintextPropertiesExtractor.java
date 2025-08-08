package com.netcracker.cloud.maas.client.impl.kafka.protocolextractors;

public class ScramPlaintextPropertiesExtractor extends ScramPropertiesExtractor {
    @Override
    protected String addressProtocolName() {
        return "PLAINTEXT";
    }

    @Override
    public int getExtractorPriority() {
        return 15;
    }
}

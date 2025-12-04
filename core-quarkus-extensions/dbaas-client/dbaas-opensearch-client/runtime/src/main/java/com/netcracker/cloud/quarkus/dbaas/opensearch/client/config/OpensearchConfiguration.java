package com.netcracker.cloud.quarkus.dbaas.opensearch.client.config;

import lombok.Getter;

import java.util.Optional;

@Getter
public class OpensearchConfiguration {
    /**
     * Property with physical database id.
     */
    private final Optional<String> physicalDatabaseId;

    /**
     * Prefix with delimiter for migration
     */
    private final SinglePrefix prefixConfig;

    public OpensearchConfiguration(Optional<String> physicalDatabaseId,
                                   SinglePrefix prefixConfig) {
        this.physicalDatabaseId = physicalDatabaseId;
        this.prefixConfig = prefixConfig;
    }
}

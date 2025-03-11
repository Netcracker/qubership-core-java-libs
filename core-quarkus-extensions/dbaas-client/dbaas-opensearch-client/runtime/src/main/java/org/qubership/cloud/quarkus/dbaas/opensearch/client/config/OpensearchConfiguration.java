package org.qubership.cloud.quarkus.dbaas.opensearch.client.config;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

import java.util.Optional;

@ConfigGroup
public class OpensearchConfiguration {
    /**
     * Property with physical database id.
     */
    @ConfigItem(name = "physical-database-id")
    public Optional<String> physicalDatabaseId;

    /**
     * Prefix with delimiter for migration
     */
    @ConfigItem(name = "prefix-config")
    public SinglePrefix prefixConfig;

}
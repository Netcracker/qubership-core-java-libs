package org.qubership.cloud.quarkus.dbaas.opensearch.client.config;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import lombok.Getter;

import java.util.Optional;

@ConfigGroup
@Getter
public class SinglePrefix {
    /**
     * delimiter between prefix and uniq name
     */
    @ConfigItem(name = "delimiter", defaultValue = "_")
    String delimiter;

    /**
     * prefix before delimiter and uniq name
     */
    @ConfigItem(name = "prefix")
    Optional<String> prefix;
}

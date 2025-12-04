package com.netcracker.cloud.quarkus.dbaas.opensearch.client.config;

import io.smallrye.config.WithDefault;

import java.util.Optional;

public interface SinglePrefix {
    /**
     * delimiter between prefix and uniq name
     */
    @WithDefault("_")
    String delimiter();

    /**
     * prefix before delimiter and uniq name
     */
    Optional<String> prefix();
}

package com.netcracker.cloud.quarkus.dbaas.cassandraclient.config.properties.migration;

import com.netcracker.cloud.dbaas.client.cassandra.migration.model.settings.SchemaAgreementSettings;

import java.util.Optional;

public interface SchemaAgreementProperties {

    /**
     * Retry delay for schema agreement await
     */
    Optional<Long> awaitRetryDelay();

    default SchemaAgreementSettings toSchemaAgreementSettings() {
        SchemaAgreementSettings.SchemaAgreementSettingsBuilder builder = SchemaAgreementSettings.builder();
        awaitRetryDelay().ifPresent(builder::withAwaitRetryDelay);
        return builder.build();
    }
}

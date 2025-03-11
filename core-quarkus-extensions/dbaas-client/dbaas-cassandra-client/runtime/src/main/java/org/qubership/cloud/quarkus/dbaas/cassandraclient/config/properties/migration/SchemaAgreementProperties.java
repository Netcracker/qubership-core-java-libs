package org.qubership.cloud.quarkus.dbaas.cassandraclient.config.properties.migration;

import org.qubership.cloud.dbaas.client.cassandra.migration.model.settings.SchemaAgreementSettings;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.Optional;

@ConfigGroup
@Getter
@Accessors(fluent = true)
public class SchemaAgreementProperties {

    /**
     * Retry delay for schema agreement await
     */
    @ConfigItem
    Optional<Long> awaitRetryDelay;

    public SchemaAgreementSettings toSchemaAgreementSettings() {
        SchemaAgreementSettings.SchemaAgreementSettingsBuilder builder = SchemaAgreementSettings.builder();
        awaitRetryDelay.ifPresent(builder::withAwaitRetryDelay);
        return builder.build();
    }
}

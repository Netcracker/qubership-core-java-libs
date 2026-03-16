package com.netcracker.cloud.quarkus.dbaas.cassandraclient.config.properties.migration;

import com.netcracker.cloud.dbaas.client.cassandra.migration.model.settings.TemplateSettings;

import java.util.Optional;

public interface TemplateProperties {

    /**
     * Resource path to get additional definitions to import into FreeMarker configuration and
     * allow to be used in schema version scripts under fn namespace
     */
    Optional<String> definitionsResourcePath();

    default TemplateSettings toTemplateSettings() {
        TemplateSettings.TemplateSettingsBuilder builder = TemplateSettings.builder();
        definitionsResourcePath().ifPresent(builder::withDefinitionsResourcePath);
        return builder.build();
    }
}

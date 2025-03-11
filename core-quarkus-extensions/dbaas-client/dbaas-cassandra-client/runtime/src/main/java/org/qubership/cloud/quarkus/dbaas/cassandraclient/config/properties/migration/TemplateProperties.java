package org.qubership.cloud.quarkus.dbaas.cassandraclient.config.properties.migration;

import org.qubership.cloud.dbaas.client.cassandra.migration.model.settings.TemplateSettings;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.Optional;

@ConfigGroup
@Getter
@Accessors(fluent = true)
public class TemplateProperties {

    /**
     * Resource path to get additional definitions to import into FreeMarker configuration and
     * allow to be used in schema version scripts under fn namespace
     */
    @ConfigItem
    Optional<String> definitionsResourcePath;

    public TemplateSettings toTemplateSettings() {
        TemplateSettings.TemplateSettingsBuilder builder = TemplateSettings.builder();
        definitionsResourcePath.ifPresent(builder::withDefinitionsResourcePath);
        return builder.build();
    }
}

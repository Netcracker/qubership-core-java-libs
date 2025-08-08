package com.netcracker.cloud.dbaas.client.cassandra.migration.model.settings;

import static com.netcracker.cloud.dbaas.client.cassandra.migration.SchemaMigrationCommonConstants.DEFAULT_DEFINITIONS_RESOURCE_PATH;

/**
 * Schema migration template settings.
 * Use {@link TemplateSettingsBuilder} for creation.
 *
 * @param definitionsResourcePath resource path to get additional definitions to import into FreeMarker configuration and
 *                                allow to be used in schema version scripts under fn namespace
 */
public record TemplateSettings(
        String definitionsResourcePath
) {

    public static TemplateSettingsBuilder builder() {
        return new TemplateSettingsBuilder();
    }

    public static class TemplateSettingsBuilder {
        private String definitionsResourcePath = DEFAULT_DEFINITIONS_RESOURCE_PATH;

        public TemplateSettingsBuilder withDefinitionsResourcePath(
                String definitionsResourcePath
        ) {
            if (definitionsResourcePath != null)
                this.definitionsResourcePath = definitionsResourcePath;
            return this;
        }

        public TemplateSettings build() {
            return new TemplateSettings(definitionsResourcePath);
        }
    }
}

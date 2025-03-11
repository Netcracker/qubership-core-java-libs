package org.qubership.cloud.core.quarkus.dbaas.datasource.config.properties;

import org.qubership.cloud.dbaas.client.entity.settings.PostgresSettings;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.config.spi.Converter;

import java.util.Arrays;

public class PostgresSettingsConverter implements Converter<PostgresSettings> {

    @Override
    public PostgresSettings convert(String value) {
        if (StringUtils.isEmpty(value)) {
            return null;
        }
        PostgresSettings settings = new PostgresSettings();
        settings.setPgExtensions(Arrays.asList(value.split(",")));

        return settings;
    }
}

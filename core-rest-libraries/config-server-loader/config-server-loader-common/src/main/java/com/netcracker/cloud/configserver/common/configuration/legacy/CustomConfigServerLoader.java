package com.netcracker.cloud.configserver.common.configuration.legacy;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.cloud.config.client.ConfigClientProperties;
import org.springframework.cloud.config.client.ConfigServicePropertySourceLocator;

@Deprecated // will be deleted in 4.x
public class CustomConfigServerLoader extends ConfigServicePropertySourceLocator {
    private static Log logger = LogFactory
            .getLog(ConfigServicePropertySourceLocator.class);

    public CustomConfigServerLoader(ConfigClientProperties defaultProperties) {
        super(defaultProperties);
    }

    @Override
    public org.springframework.core.env.PropertySource<?> locate(
            org.springframework.core.env.Environment environment) {
        logger.warn("Detected legacy bootstrap initialization. But config-server properties will be loaded by the new way.");
        return null;
    }


}

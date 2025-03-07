package org.qubership.cloud.configserver.common.configuration;

import org.springframework.boot.DefaultPropertiesPropertySource;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.cloud.config.client.ConfigClientProperties;
import org.springframework.cloud.config.client.ConfigServerConfigDataLocationResolver;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

import static org.qubership.cloud.configserver.common.configuration.AbstractCustomConfigServerConfigDataLocationResolver.INNER_CONFIG_SERVER_LOCATION_PREFIX;

public class BeforeConfigServerDataLoaderEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {
    public static final int ORDER = ConfigDataEnvironmentPostProcessor.ORDER - 1;
    private static final String DEFAULT_CONFIG_SERVER_URL = "http://config-server:8080";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        boolean configEnabled = environment.getProperty(ConfigClientProperties.PREFIX + ".enabled", Boolean.class, true);
        if (!configEnabled) {
            return;
        }
        String property = environment.getProperty("spring.config.import"); // for example from system properties or env
        if (!StringUtils.hasText(property)) {
            // Inserting inner config server location in order to don't specify spring.config.import=configserver: in configuration file. We have done it for backward compatibility.
            setConfigServerImportProperty(environment);
            return;
        }
        // if user specify spring.config.import but didn't mention configserver then we should throw exception because we don't know exact location order.
        if (!property.contains(ConfigServerConfigDataLocationResolver.PREFIX)) {
            throw new IllegalStateException("Found spring.config.import property but without configserver: value. Please add configserver data import parameter to your config file");
        }
    }

    private void setConfigServerImportProperty(ConfigurableEnvironment environment) {
        Map<String, Object> data = new HashMap<>();
        data.put("spring.config.import", "optional:" + INNER_CONFIG_SERVER_LOCATION_PREFIX + DEFAULT_CONFIG_SERVER_URL);
        DefaultPropertiesPropertySource.addOrMerge(data, environment.getPropertySources());
    }

    @Override
    public int getOrder() {
        return ORDER;
    }

}

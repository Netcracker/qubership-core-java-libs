package com.netcracker.cloud.configserver.common.configuration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.boot.env.DefaultPropertiesPropertySource;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.cloud.config.client.ConfigClientProperties;
import org.springframework.cloud.config.client.ConfigServerConfigDataLocationResolver;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static com.netcracker.cloud.configserver.common.configuration.AbstractCustomConfigServerConfigDataLocationResolver.INNER_CONFIG_SERVER_LOCATION_PREFIX;

public class AfterConfigServerDataLoaderEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {
    public static final int ORDER = ConfigDataEnvironmentPostProcessor.ORDER + 1;
    private static final String SPRING_CONFIG_IMPORT ="spring.config.import";
    private static final String DEFAULT_CONFIG_SERVER_URL = "http://config-server:8080";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        boolean configEnabled = environment.getProperty(ConfigClientProperties.PREFIX + ".enabled", Boolean.class, true);
        if (!configEnabled) {
            return;
        }
        String property = environment.getProperty(SPRING_CONFIG_IMPORT);
        if (StringUtils.hasLength(property)) {
            String[] locations = property.split(",");
            Optional<String> defaultTarget = Arrays.stream(locations)
                    .filter(loc -> loc.contains(INNER_CONFIG_SERVER_LOCATION_PREFIX) || loc.contains(ConfigServerConfigDataLocationResolver.PREFIX))
                    .findFirst();

            if (defaultTarget.isPresent()) {
                addToDefault(defaultTarget.get(), environment);
                return;
            }

            boolean isThereConfigServerLocation = Arrays.stream(locations).anyMatch(location -> location.contains(ConfigServerConfigDataLocationResolver.PREFIX));
            if (!isThereConfigServerLocation) {
                throw new IllegalStateException("Found spring.config.import property but without " + ConfigServerConfigDataLocationResolver.PREFIX + " value. " +
                        "Please add a spring.config.import=configserver:" + DEFAULT_CONFIG_SERVER_URL + " property to your configuration file.");
            }
        }
    }

    /**
     * set spring.config.import default value
     */
    private void addToDefault(String configServerLocation, ConfigurableEnvironment environment) {
        if (configServerLocation.contains(INNER_CONFIG_SERVER_LOCATION_PREFIX)) { // Rewrite synthetic location
            DefaultPropertiesPropertySource.addOrMerge(Collections.singletonMap(SPRING_CONFIG_IMPORT, "optional:configserver:" + DEFAULT_CONFIG_SERVER_URL), environment.getPropertySources());
            return;
        }
        DefaultPropertiesPropertySource.addOrMerge(Collections.singletonMap(SPRING_CONFIG_IMPORT, configServerLocation), environment.getPropertySources());
    }

    @Override
    public int getOrder() {
        return ORDER;
    }

}

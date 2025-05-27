package org.qubership.cloud.configserver.common.configuration;


import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultPropertiesPropertySource;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.core.env.PropertySource;
import org.springframework.mock.env.MockEnvironment;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AfterConfigServerDataLoaderEnvironmentPostProcessorTest {

    private static final String SPRING_CONFIG_IMPORT = "spring.config.import";

    private MockEnvironment getEnvironment() {
        return new MockEnvironment();
    }

    private AfterConfigServerDataLoaderEnvironmentPostProcessor configServerDataLoaderEnvironmentPostProcessor =
            new AfterConfigServerDataLoaderEnvironmentPostProcessor();

    @Test
    void orderMustBeBeforeConfigDataEnvironmentPostProcessor() {
        assertThat(AfterConfigServerDataLoaderEnvironmentPostProcessor.ORDER).isGreaterThan(ConfigDataEnvironmentPostProcessor.ORDER);
    }

    @Test
    void changeInnerConfigServerPrefix() {
        Map<String, Object> props = new HashMap<>();
        props.put(SPRING_CONFIG_IMPORT, "optional:confserv:http://config-server:8080");
        MockEnvironment environment = getEnvironment();
        DefaultPropertiesPropertySource.addOrMerge(props, environment.getPropertySources());
        configServerDataLoaderEnvironmentPostProcessor.postProcessEnvironment(environment, null);
        String property = environment.getProperty(SPRING_CONFIG_IMPORT);
        assertThat(property)
                .doesNotContain("optional:confserv:http://config-server:8080")
                .contains("optional:configserver:http://config-server:8080");
    }

    @Test
    void exceptionIfUserDoesNotPassConfigServerLocation() {
        MockEnvironment environment = getEnvironment();
        environment.setProperty(SPRING_CONFIG_IMPORT, "my_config_data");
        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                configServerDataLoaderEnvironmentPostProcessor.postProcessEnvironment(environment, null)
        );
        assertThat(exception).hasMessage("Found spring.config.import property but without configserver: value. " +
                "Please add a spring.config.import=configserver:http://config-server:8080 property to your configuration file.");

    }

    @Test
    void configImportPropertyMustBeInDefaultsAfterPostProcess() {
        MockEnvironment environment = getEnvironment();
        environment.setProperty(SPRING_CONFIG_IMPORT, "configserver:http://config-server:8080,zookeeper:");

        Assertions.assertNull(environment.getPropertySources().get(DefaultPropertiesPropertySource.NAME));
        configServerDataLoaderEnvironmentPostProcessor.postProcessEnvironment(environment, null);

        PropertySource defaultPropertySource = environment.getPropertySources().get(DefaultPropertiesPropertySource.NAME);
        Assertions.assertNotNull(defaultPropertySource);
        Assertions.assertTrue(defaultPropertySource.containsProperty(SPRING_CONFIG_IMPORT));
        Assertions.assertEquals("configserver:http://config-server:8080", defaultPropertySource.getProperty(SPRING_CONFIG_IMPORT));
    }
}

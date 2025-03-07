package org.qubership.cloud.configserver.common.configuration;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.DefaultPropertiesPropertySource;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.cloud.config.client.ConfigClientProperties;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class BeforeConfigServerDataLoaderEnvironmentPostProcessorTest {


    private static final String SPRING_CONFIG_IMPORT = "spring.config.import";

    private MockEnvironment getEnvironment() {
        return new MockEnvironment();
    }

    private BeforeConfigServerDataLoaderEnvironmentPostProcessor configServerDataLoaderEnvironmentPostProcessor =
            new BeforeConfigServerDataLoaderEnvironmentPostProcessor();

    @Test
    void orderMustBeBeforeConfigDataEnvironmentPostProcessor() {
        assertThat(BeforeConfigServerDataLoaderEnvironmentPostProcessor.ORDER).isLessThan(ConfigDataEnvironmentPostProcessor.ORDER);
    }

    @Test
    void checkDisableConfigServerProperty() {
        MockEnvironment spyEnv = Mockito.spy(MockEnvironment.class);
        spyEnv.setProperty(ConfigClientProperties.PREFIX + ".enabled", "true");
        configServerDataLoaderEnvironmentPostProcessor.postProcessEnvironment(spyEnv, null);
        verify(spyEnv, times(1)).getProperty(anyString());
    }

    @Test
    void setSpringConfigImportIfAbsent() {
        ConfigurableEnvironment environment = getEnvironment();
        configServerDataLoaderEnvironmentPostProcessor.postProcessEnvironment(environment, null);
        PropertySource<?> propertySource = environment.getPropertySources().get(DefaultPropertiesPropertySource.NAME);
        String property = (String) propertySource.getProperty(SPRING_CONFIG_IMPORT);
        assertThat(property).isEqualTo("optional:confserv:http://config-server:8080");
    }

    @Test
    void exceptionIfSpringConfigImportWithoutConfigServer() {
        MockEnvironment environment = getEnvironment();
        environment.setProperty(SPRING_CONFIG_IMPORT, "my_config_data");
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            configServerDataLoaderEnvironmentPostProcessor.postProcessEnvironment(environment, null);
        });
        assertThat(exception).hasMessage("Found spring.config.import property but without configserver: value. Please add configserver data import parameter to your config file");

    }
}
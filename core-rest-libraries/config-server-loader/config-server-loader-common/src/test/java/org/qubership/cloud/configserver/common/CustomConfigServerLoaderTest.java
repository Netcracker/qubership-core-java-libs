package org.qubership.cloud.configserver.common;

import org.qubership.cloud.configserver.common.configuration.CustomConfigServerDataLoader;
import org.qubership.cloud.restclient.MicroserviceRestClient;
import org.qubership.cloud.restclient.entity.RestClientResponseEntity;
import org.qubership.cloud.restclient.exception.MicroserviceRestClientException;
import org.qubership.cloud.restclient.exception.MicroserviceRestClientResponseException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.boot.ConfigurableBootstrapContext;
import org.springframework.boot.context.config.ConfigData;
import org.springframework.boot.context.config.ConfigDataLoaderContext;
import org.springframework.boot.context.config.Profiles;
import org.springframework.boot.context.properties.bind.*;
import org.springframework.cloud.config.client.ConfigClientProperties;
import org.springframework.cloud.config.client.ConfigServerConfigDataResource;
import org.springframework.cloud.config.environment.Environment;
import org.springframework.core.env.PropertySource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.boot.context.properties.bind.BindResult;
import org.springframework.web.client.HttpServerErrorException;

import java.util.Collections;

@RunWith(MockitoJUnitRunner.class)
public class CustomConfigServerLoaderTest {

    private CustomConfigServerDataLoader customConfigServerDataLoader;
    private RestClientResponseEntity<Environment> restClientResponseEntity;

    @Mock
    ConfigClientProperties configClientProperties;

    @Mock
    MicroserviceRestClient microserviceRestClient;

    @Before
    public void before() {
        customConfigServerDataLoader = new CustomConfigServerDataLoader();
    }

    @Test
    public void locateMainActivityTest() throws Exception {

        Environment environment = new Environment("test", "dev");
        environment.setVersion("test-version");
        environment.setState("test-state");
        environment.add(new org.springframework.cloud.config.environment.PropertySource("test", Collections.singletonMap("testKey", "testValue")));
        restClientResponseEntity = new RestClientResponseEntity<Environment>(environment, 200);

        Mockito.doReturn(restClientResponseEntity)
                .when(microserviceRestClient).doRequest(Mockito.any(),
                Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());


        ConfigData configData = getConfigData(microserviceRestClient, createConfigClientProperties());

        byte count = 0;
        for (PropertySource propertySource : configData.getPropertySources()) {
            if (propertySource.getName().equals("configClient")) {
                Assert.assertEquals("test-state", propertySource.getProperty("config.client.state"));
                Assert.assertEquals("test-version", propertySource.getProperty("config.client.version"));
                count++;
            }
            if (propertySource.getName().equals("configserver:test")) {
                Assert.assertEquals("testValue", propertySource.getProperty("testKey"));
                count++;
            }
        }

        Assert.assertEquals(2, count);

    }

    private ConfigData getConfigData(MicroserviceRestClient microserviceRestClient, ConfigClientProperties configClientProperties) {
        Binder binder = Mockito.mock(Binder.class);
        BindHandler mockBindHandler = getMockBindHandler(binder);
        BindResult<String> bindResult = Mockito.mock(BindResult.class);
        ConfigDataLoaderContext configDataLoaderContext = Mockito.mock(ConfigDataLoaderContext.class);
        ConfigurableBootstrapContext configurableBootstrapContext = Mockito.mock(ConfigurableBootstrapContext.class);
        Mockito.when(configurableBootstrapContext.getOrElse(BindHandler.class, null)).thenReturn(mockBindHandler);
        Mockito.when(configurableBootstrapContext.getOrElse(Binder.class, null)).thenReturn(binder);
        Mockito.when(configurableBootstrapContext.get(MicroserviceRestClient.class)).thenReturn(microserviceRestClient);
        Mockito.when(configurableBootstrapContext.get(Binder.class)).thenReturn(binder);
        Mockito.when(binder.bind("cloud.microservice.name", String.class)).thenReturn(bindResult);
        Mockito.when(configDataLoaderContext.getBootstrapContext()).thenReturn(configurableBootstrapContext);
        Profiles profiles = Mockito.mock(Profiles.class);

        ConfigServerConfigDataResource configServerConfigDataResource = new ConfigServerConfigDataResource(configClientProperties, true, profiles);
        ConfigData configData = customConfigServerDataLoader.doLoad(configDataLoaderContext, configServerConfigDataResource);
        return configData;
    }


    private BindHandler getMockBindHandler(Binder binder) {
        BindResult<Integer> integerBindResult = Mockito.mock(BindResult.class);
        Mockito.when(integerBindResult.orElse(12)).thenReturn(12);
        BindResult<Long> longBindResult = Mockito.mock(BindResult.class);
        Mockito.when(longBindResult.orElse(5000L)).thenReturn(100L);
        BindHandler testBindHandler = new TestBindHandler();
        Mockito.when(binder.bind("core.spring.cloud.config.retry.max-attempts", Bindable.of(Integer.class), testBindHandler)).thenReturn(integerBindResult);
        Mockito.when(binder.bind("core.spring.cloud.config.retry.max-interval-ms", Bindable.of(Long.class), testBindHandler)).thenReturn(longBindResult);
        return testBindHandler;
    }

    @Test(expected = IllegalStateException.class)
    public void throwHttpException() {
        HttpHeaders httpHeader = new HttpHeaders();
        httpHeader.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        Mockito.doThrow(new HttpServerErrorException(HttpStatus.BAD_REQUEST, "", httpHeader, "httpBody".getBytes(), null))
                .when(microserviceRestClient).doRequest(Mockito.any(),
                Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
        ConfigClientProperties configClientProperties = createConfigClientProperties();
        configClientProperties.setFailFast(true);
        getConfigData(microserviceRestClient, configClientProperties);
    }

    @Test
    public void catchMicroserviceRestClientResponseException() {
        Mockito.doThrow(new MicroserviceRestClientResponseException("test", 400, "test-body".getBytes(), Collections.emptyMap())).when(microserviceRestClient).doRequest(Mockito.any(),
                Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
        Assert.assertNull(getConfigData(microserviceRestClient, createConfigClientProperties()));
    }

    @Test
    public void testWillRetryTwelveTimesIfConfigserverIsNotAvailableAndMicroserviceRestClientResponseException() {
        Mockito.doThrow(new MicroserviceRestClientResponseException("test", 400, "test-body".getBytes(), Collections.emptyMap())).when(microserviceRestClient).doRequest(Mockito.any(),
                Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
        Assert.assertNull(getConfigData(microserviceRestClient, createConfigClientProperties()));
        Mockito.verify(microserviceRestClient, Mockito.times(12)).doRequest(Mockito.any(),
                Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
    }

    @Test
    public void testWillRetryTwelveTimesIfConfigserverIsNotAvailableAndMicroserviceRestClientException() {
        Mockito.doThrow(new MicroserviceRestClientException("Error during request")).when(microserviceRestClient).doRequest(Mockito.any(),
                Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
        Assert.assertNull(getConfigData(microserviceRestClient, createConfigClientProperties()));
        Mockito.verify(microserviceRestClient, Mockito.times(12)).doRequest(Mockito.any(),
                Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
    }

    private ConfigClientProperties createConfigClientProperties() {
        ConfigClientProperties configClient = new ConfigClientProperties(new MockEnvironment());
        configClient.setName("test");
        configClient.setLabel("test-label");
        return configClient;
    }

    public static class TestBindHandler extends AbstractBindHandler {

    }
}

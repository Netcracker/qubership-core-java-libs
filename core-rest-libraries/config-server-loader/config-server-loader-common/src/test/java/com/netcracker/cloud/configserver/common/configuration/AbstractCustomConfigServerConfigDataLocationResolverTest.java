package com.netcracker.cloud.configserver.common.configuration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.bootstrap.DefaultBootstrapContext;
import org.springframework.boot.context.config.ConfigDataLocation;
import org.springframework.boot.context.config.ConfigDataLocationResolverContext;
import org.springframework.boot.context.config.Profiles;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.cloud.config.client.ConfigServerConfigDataResource;
import org.springframework.mock.env.MockEnvironment;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AbstractCustomConfigServerConfigDataLocationResolverTest {

    private AbstractCustomConfigServerConfigDataLocationResolver abstractConfigDataLocationResolver;
    private ConfigDataLocationResolverContext context = mock(ConfigDataLocationResolverContext.class);

    private MockEnvironment environment;

    private Binder environmentBinder;

    @BeforeEach
    public void before() {
        this.environment = new MockEnvironment();
        this.environmentBinder = Binder.get(this.environment);
        when(context.getBinder()).thenReturn(environmentBinder);
        when(context.getBootstrapContext()).thenReturn(new DefaultBootstrapContext());
        abstractConfigDataLocationResolver = mock(AbstractCustomConfigServerConfigDataLocationResolver.class, Mockito.CALLS_REAL_METHODS);
    }


    @Test
    void isResolvableWhenInnerConfigServerPrefix() {
        boolean resolvable = abstractConfigDataLocationResolver.isResolvable(context, ConfigDataLocation.of("optional:confserv:http://config-server:8080"));
        assertThat(resolvable).isTrue();
    }

    @Test
    void isResolvableWhenConfigServerPrefix() {
        boolean resolvable = abstractConfigDataLocationResolver.isResolvable(context, ConfigDataLocation.of("optional:configserver:http://config-server:8080"));
        assertThat(resolvable).isTrue();
    }

    @Test
    void configDataResourceListMustBeEmpty() {
        boolean resolvable = abstractConfigDataLocationResolver.isResolvable(context,  ConfigDataLocation.of("optional:configserver:http://config-server:8080"));
        List<ConfigServerConfigDataResource> configServerConfigDataResources =
                abstractConfigDataLocationResolver.resolveProfileSpecific(null,  ConfigDataLocation.of("optional:confserv:http://config-server:8080"), null);
        assertThat(configServerConfigDataResources).isEmpty();
    }

    @Test
    void checkInnerSpecificConfigServerDataResource(){
        Profiles profiles = mock(Profiles.class);
        List<ConfigServerConfigDataResource> configServerConfigDataResources =
                abstractConfigDataLocationResolver.resolveProfileSpecific(context,  ConfigDataLocation.of("optional:confserv:http://config-server:8080"), profiles);

        assertThat(configServerConfigDataResources).allMatch(configServerConfigDataResource ->
                Arrays.stream(configServerConfigDataResource.getProperties().getUri()).allMatch(url -> url.equals("http://config-server:8080")));
        assertThat(configServerConfigDataResources).allMatch(configServerConfigDataResource ->
                configServerConfigDataResource.getProperties().isFailFast());
    }

    @Test
    void checkConfigServerDataResource(){
        Profiles profiles = mock(Profiles.class);
        List<ConfigServerConfigDataResource> configServerConfigDataResources =
                abstractConfigDataLocationResolver.resolveProfileSpecific(context,  ConfigDataLocation.of("optional:configserver:http://config-server:8080"), profiles);

        assertThat(configServerConfigDataResources).allMatch(configServerConfigDataResource ->
                Arrays.stream(configServerConfigDataResource.getProperties().getUri()).allMatch(url -> url.equals("http://config-server:8080")));
        assertThat(configServerConfigDataResources).allMatch(configServerConfigDataResource ->
                !configServerConfigDataResource.getProperties().isFailFast());
    }

}

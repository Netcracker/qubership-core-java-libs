package com.netcracker.cloud.configserver.common.configuration;

import com.netcracker.cloud.restclient.MicroserviceRestClient;
import org.apache.commons.logging.Log;
import org.springframework.boot.BootstrapRegistry;
import org.springframework.boot.context.config.ConfigDataLocation;
import org.springframework.boot.context.config.ConfigDataLocationNotFoundException;
import org.springframework.boot.context.config.ConfigDataLocationResolverContext;
import org.springframework.boot.context.config.Profiles;
import org.springframework.boot.logging.DeferredLogFactory;
import org.springframework.cloud.config.client.ConfigClientProperties;
import org.springframework.cloud.config.client.ConfigServerConfigDataLocationResolver;
import org.springframework.cloud.config.client.ConfigServerConfigDataResource;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;

public abstract class AbstractCustomConfigServerConfigDataLocationResolver extends ConfigServerConfigDataLocationResolver {

    public static final String INNER_CONFIG_SERVER_LOCATION_PREFIX = "confserv:";
    private boolean isConfigLocationRegistered = false;

    public AbstractCustomConfigServerConfigDataLocationResolver(DeferredLogFactory log) {
        super(log);
    }

    @Override
    public int getOrder() {
        return super.getOrder() - 1;
    }


    @Override
    public boolean isResolvable(ConfigDataLocationResolverContext context, ConfigDataLocation location) {
        if (location.hasPrefix(INNER_CONFIG_SERVER_LOCATION_PREFIX)) {
            return true;
        }
        boolean resolvable = super.isResolvable(context, location);
        if (location.hasPrefix(PREFIX) && resolvable) {
            isConfigLocationRegistered = true;
        }
        return resolvable;
    }

    @Override
    public List<ConfigServerConfigDataResource> resolveProfileSpecific(ConfigDataLocationResolverContext resolverContext, ConfigDataLocation location, Profiles profiles) throws ConfigDataLocationNotFoundException {
        if (isConfigLocationRegistered && location.hasPrefix(INNER_CONFIG_SERVER_LOCATION_PREFIX)) {
            return Collections.emptyList();
        }
        resolverContext.getBootstrapContext().registerIfAbsent(MicroserviceRestClient.class, BootstrapRegistry.InstanceSupplier.of(getMicroserviceRestClient()));
        List<ConfigServerConfigDataResource> configServerConfigDataResources = super.resolveProfileSpecific(resolverContext, location, profiles);

        if (location.hasPrefix(INNER_CONFIG_SERVER_LOCATION_PREFIX)) {
            setCorrectUrl(configServerConfigDataResources, location);
            setFailFast(configServerConfigDataResources);
        }

        return configServerConfigDataResources;
    }

    private void setFailFast(List<ConfigServerConfigDataResource> configServerConfigDataResources) {
        configServerConfigDataResources.forEach(configServerConfigDataResource -> configServerConfigDataResource.getProperties().setFailFast(true));
    }

    private void setCorrectUrl(List<ConfigServerConfigDataResource> configServerConfigDataResources, ConfigDataLocation location) {
        for (ConfigServerConfigDataResource configServerConfigDataResource : configServerConfigDataResources) {
            ConfigClientProperties properties = configServerConfigDataResource.getProperties();
            String uris = location.getNonPrefixedValue(INNER_CONFIG_SERVER_LOCATION_PREFIX);
            if (StringUtils.hasText(uris)) {
                String[] uri = StringUtils.commaDelimitedListToStringArray(uris);
                properties.setUri(uri);
            }
        }
    }

    public abstract MicroserviceRestClient getMicroserviceRestClient();
}

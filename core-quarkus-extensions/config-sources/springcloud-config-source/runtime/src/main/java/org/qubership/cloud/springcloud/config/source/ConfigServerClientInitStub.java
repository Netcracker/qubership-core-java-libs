package org.qubership.cloud.springcloud.config.source;

import com.google.common.collect.Maps;

import java.util.Collections;
import java.util.Map;

public class ConfigServerClientInitStub implements ConfigServerClient {

    public final static CloudEnv EMPTY_CLOUD_ENV = new CloudEnv();

    public static class PropertyManagerHolder {
        public static final ConfigServerClientInitStub holder = new ConfigServerClientInitStub();
    }

    public static ConfigServerClientInitStub getEmptyConfigServerClient() {
        return ConfigServerClientInitStub.PropertyManagerHolder.holder;
    }

    public ConfigServerClientInitStub() {
        PropertySource propertySource = new PropertySource();
        propertySource.setSource(Maps.newHashMapWithExpectedSize(0));
        EMPTY_CLOUD_ENV.setPropertySources(Collections.singletonList(propertySource));
    }

    public CloudEnv getProperties() {
        return EMPTY_CLOUD_ENV;
    }

    public void putProperties(Map<String, String> properties) {

    }
}

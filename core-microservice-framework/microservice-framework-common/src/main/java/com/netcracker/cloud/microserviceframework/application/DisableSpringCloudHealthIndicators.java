package com.netcracker.cloud.microserviceframework.application;

import org.springframework.boot.context.event.ApplicationContextInitializedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertiesPropertySource;

import java.util.Properties;

public class DisableSpringCloudHealthIndicators implements ApplicationListener<ApplicationContextInitializedEvent> {

    private final String discoveryPropertyName = "spring.cloud.discovery.client.health-indicator.enabled";
    private final String FALSE = "false";
    private final String defaultPropertiesName = "default-runtime-properties";

    public void onApplicationEvent(ApplicationContextInitializedEvent applicationContextInitializedEvent) {
        ConfigurableEnvironment environment = applicationContextInitializedEvent.getApplicationContext().getEnvironment();
        if (!environment.containsProperty(discoveryPropertyName)){
            Properties props = new Properties();
            props.put(discoveryPropertyName, FALSE);
            environment.getPropertySources().addFirst(new PropertiesPropertySource(defaultPropertiesName, props));
        }
    }
}

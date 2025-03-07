package org.qubership.cloud.frameworkextensions.swagger.config;


import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.boot.logging.DeferredLog;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * This class is used to create {@link SwaggerSecurityConfigurator}
 * bean, which will configure authorization in Swagger UI.
 */
@Slf4j
public class SwaggerSecurityConfigurator implements EnvironmentPostProcessor, ApplicationListener<ApplicationEvent> {
    private static final String CLIENT_ID_FILENAME = "username";
    private static final String CLIENT_SECRET_FILENAME = "password";
    private static final String PROPERTY_SOURCE_NAME = "defaultProperties";
    public static final String SPRINGDOC_SWAGGER_UI_OAUTH_CLIENT_ID = "springdoc.swagger-ui.oauth.clientId";
    public static final String SPRINGDOC_SWAGGER_UI_OAUTH_CLIENT_SECRET = "springdoc.swagger-ui.oauth.clientSecret";

    @Value("${SECRET_PATH:/etc/secret}")
    private String secretPath;

    @Value("${cloud.microservice.name:unknown}")
    private String appName;

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment,
                                       SpringApplication application) {
        Map<String, Object> map = new HashMap<>();
        map.put(SPRINGDOC_SWAGGER_UI_OAUTH_CLIENT_ID, clientId());
        map.put(SPRINGDOC_SWAGGER_UI_OAUTH_CLIENT_SECRET, clientSecret());
        addOrReplace(environment.getPropertySources(), map);
    }

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        new DeferredLog().replayTo(SwaggerSecurityConfigurator.class);
    }

    private String clientId() {
        String filePath = secretPath + "/" + CLIENT_ID_FILENAME;
        String clientId;
        try {
            clientId = new String(Files.readAllBytes(Paths.get(filePath)));
        } catch (IOException e) {
            clientId = appName;
            log.warn("Swagger Configuration couldn't read clientId from file {}, using default: {}", filePath, clientId);
        }
        return clientId;
    }

    private String clientSecret() {
        String filePath = secretPath + "/" + CLIENT_SECRET_FILENAME;
        String clientSecret = null;
        try {
            clientSecret = new String(Files.readAllBytes(Paths.get(filePath)));
        } catch (IOException e) {
            log.warn("Swagger Configuration couldn't read clientSecret from file {}", filePath);
        }
        return clientSecret;
    }

    private void addOrReplace(MutablePropertySources propertySources,
                              Map<String, Object> map) {
        MapPropertySource target = null;
        if (propertySources.contains(PROPERTY_SOURCE_NAME)) {
            PropertySource<?> source = propertySources.get(PROPERTY_SOURCE_NAME);
            if (source instanceof MapPropertySource) {
                target = (MapPropertySource) source;
                for (String key : map.keySet()) {
                    if (!target.containsProperty(key)) {
                        target.getSource().put(key, map.get(key));
                    }
                }
            }
        }
        if (target == null) {
            target = new MapPropertySource(PROPERTY_SOURCE_NAME, map);
        }
        if (!propertySources.contains(PROPERTY_SOURCE_NAME)) {
            propertySources.addLast(target);
        }
    }
}

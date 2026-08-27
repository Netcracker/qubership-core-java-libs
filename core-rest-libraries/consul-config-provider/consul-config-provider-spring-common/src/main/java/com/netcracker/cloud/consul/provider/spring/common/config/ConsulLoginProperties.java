package com.netcracker.cloud.consul.provider.spring.common.config;

import com.netcracker.cloud.consul.provider.common.ConsulLoginMode;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Inputs of the Consul ACL token login. Every field stays {@code null} when the property is absent: the defaults
 * belong to {@code TokenStorageFactory.CreateOptions.Builder}, so that an external caller of the builder gets them
 * too.
 */
//todo vlla lombok?
@ConfigurationProperties(prefix = ConsulLoginProperties.PREFIX)
public class ConsulLoginProperties {

    public static final String PREFIX = "spring.cloud.consul.config.login";

    private ConsulLoginMode mode;
    private String authMethod;
    private String audience;

    public ConsulLoginMode getMode() {
        return mode;
    }

    public void setMode(ConsulLoginMode mode) {
        this.mode = mode;
    }

    public String getAuthMethod() {
        return authMethod;
    }

    public void setAuthMethod(String authMethod) {
        this.authMethod = authMethod;
    }

    public String getAudience() {
        return audience;
    }

    public void setAudience(String audience) {
        this.audience = audience;
    }
}

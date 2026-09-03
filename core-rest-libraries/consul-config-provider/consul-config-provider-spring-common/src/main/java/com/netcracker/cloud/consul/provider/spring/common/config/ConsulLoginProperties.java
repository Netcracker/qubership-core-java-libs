package com.netcracker.cloud.consul.provider.spring.common.config;

import com.netcracker.cloud.consul.provider.common.ConsulLoginMode;
import com.netcracker.cloud.consul.provider.common.TokenStorageFactory;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Inputs of the Consul ACL token login. Every field stays {@code null} when the property is absent: the defaults
 * belong to {@code TokenStorageFactory.CreateOptions.Builder}, so that an external caller of the builder gets them
 * too.
 */
@ConfigurationProperties(prefix = ConsulLoginProperties.PREFIX)
public class ConsulLoginProperties {

    public static final String PREFIX = "spring.cloud.consul.config.login";

    private ConsulLoginMode mode;
    private String authMethod;
    private String audience;
    private Duration fallbackRecheckInterval;

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

    public Duration getFallbackRecheckInterval() {
        return fallbackRecheckInterval;
    }

    public void setFallbackRecheckInterval(Duration fallbackRecheckInterval) {
        this.fallbackRecheckInterval = fallbackRecheckInterval;
    }

    /**
     * Starts an options builder from the four login inputs, leaving the absent ones {@code null} for the builder to
     * default. The Consul URL, the namespace and the M2M token supplier stay with the caller: every entry point
     * resolves them its own way, and the ConfigData phase leaves out the last two in the {@code kubernetes} mode.
     */
    public TokenStorageFactory.CreateOptions.Builder toOptionsBuilder() {
        return new TokenStorageFactory.CreateOptions.Builder()
                .mode(mode)
                .authMethod(authMethod)
                .audience(audience)
                .fallbackRecheckInterval(fallbackRecheckInterval);
    }
}

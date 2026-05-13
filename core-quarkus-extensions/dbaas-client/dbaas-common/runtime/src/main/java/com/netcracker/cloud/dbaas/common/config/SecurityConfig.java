package com.netcracker.cloud.dbaas.common.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

@ConfigMapping(prefix = "security.m2m")
public interface SecurityConfig {
    @WithName("kubernetes.enabled")
    @WithDefault("false")
    boolean k8sEnabled();
}

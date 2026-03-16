package com.netcracker.cloud.consul.provider.spring.common;

import org.springframework.cloud.consul.ConsulProperties;

public class Utils {
    public static String formatConsulAddress(ConsulProperties properties) {
        String scheme = "http";
        if (properties.getScheme() != null) {
            scheme = properties.getScheme();
        }
        return scheme + "://" + properties.getHost() + ":" + properties.getPort();
    }
}

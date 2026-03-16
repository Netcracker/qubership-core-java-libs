package com.netcracker.cloud.bluegreen.impl.util;

import com.netcracker.cloud.bluegreen.impl.http.ResponseHandler;

import java.time.Duration;

public class ConsulUtil {

    public static Duration fromConsulTTL(String ttl) {
        ttl = ttl.toUpperCase();
        if (!ttl.startsWith("PT")) {
            return Duration.parse("PT" + ttl);
        } else {
            return Duration.parse(ttl);
        }
    }

    public static String toConsulTTL(Duration ttl) {
        return ttl.toString().replace("PT", "").toLowerCase();
    }

    public static String toConsulTTLAsSeconds(long seconds) {
        return String.format("%ds", seconds);
    }

    public static String getModifyIndex(ResponseHandler<?> response) {
        return response.getResponseHeaders().firstValue("X-Consul-Index")
                .orElseThrow(() -> new IllegalStateException("X-Consul-Index header not provided"));
    }

}

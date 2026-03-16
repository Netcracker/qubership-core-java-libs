package com.netcracker.cloud.bluegreen.impl.service;

import com.netcracker.cloud.bluegreen.impl.http.ResponseHandler;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Slf4j
class LockUtils {
    public static long getLongPollingWaitForTimeoutSeconds(Duration timeout, Instant start) {
        long waitDurationSeconds = timeout.minus(Duration.between(start, Instant.now())).toSeconds();
        if (waitDurationSeconds <= 0) {
            waitDurationSeconds = 1;
        }
        return waitDurationSeconds;
    }

    public static void checkLockTimeout(Duration timeout) throws IllegalArgumentException {
        if (timeout == null || timeout.isNegative()) {
            throw new IllegalArgumentException("timeout cannot be empty or negative");
        }
    }

    public static void checkNamespaces(List<String> namespaces) throws IllegalArgumentException {
        if (namespaces == null || namespaces.isEmpty()) {
            throw new IllegalArgumentException("namespaces list parameter cannot be empty");
        }
    }

    public static void checkNotEmpty(Map<String, Object> params) throws IllegalArgumentException {
        List<String> emptyParams = params.entrySet().stream().filter(e -> e.getValue() == null || (e.getValue() instanceof String s && s.trim().isEmpty()))
                .map(Map.Entry::getKey).toList();
        if (!emptyParams.isEmpty()) {
            throw new IllegalArgumentException(String.format("Params %s cannot be empty", String.join(",", emptyParams)));
        }
    }

    public static String getModifyIndex(ResponseHandler<?> response) {
        return response.getResponseHeaders().firstValue("X-Consul-Index")
                .orElseThrow(() -> new IllegalStateException("X-Consul-Index header not provided"));
    }

}

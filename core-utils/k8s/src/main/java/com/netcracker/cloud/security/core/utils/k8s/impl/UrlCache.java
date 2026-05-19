package com.netcracker.cloud.security.core.utils.k8s.impl;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
public class UrlCache {
    private static final String INTERNAL_GATEWAY = "internal-gateway";
    private static final int CACHE_SIZE = 400;
    private static final long CACHE_DURATION_SECONDS = TimeUnit.HOURS.toSeconds(5);
    private final Cache<String, Boolean> cache;

    public UrlCache() {
        this(CACHE_SIZE, CACHE_DURATION_SECONDS);
    }

    public UrlCache(final int cacheSize, final long ttlSeconds) {
        this.cache = Caffeine.newBuilder()
                .maximumSize(cacheSize)
                .expireAfterAccess(ttlSeconds, TimeUnit.SECONDS)
                .build();
    }

    public void store(@NotNull final String key) {
        cache.put(key, Boolean.TRUE);
    }

    public boolean containsKey(@NotNull final String key) {
        return cache.getIfPresent(key) != null;
    }

    public static String calculateCacheKey(final String rawUrl) {
        URI parsedURI;
        try {
            parsedURI = new URI(rawUrl);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Failed during parsing of URL: ", ex);
        }
        return calculateCacheKey(parsedURI);
    }

    public static String calculateCacheKey(final URI parsedURI) {
        return parsedURI.getHost().contains(INTERNAL_GATEWAY)
                ? calculateCacheKeyForInternalGateway(parsedURI)
                : parsedURI.getHost() + ":" + parsedURI.getPort();
    }

    private static String calculateCacheKeyForInternalGateway(final URI parsedUri) {
        final String[] segments = StringUtils.strip(parsedUri.getPath(), "/").split("/");
        final List<String> filteredSegments = new ArrayList<>();

        String version = "";
        String serviceName = "";

        for (String segment : segments) {
            if (StringUtils.isNotEmpty(version)) {
                serviceName = segment;
                break;
            }
            filteredSegments.add(segment);
            if (isVersion(segment)) {
                version = segment;
            }
        }

        if (StringUtils.isEmpty(version)) {
            log.debug("internal-gateway url does not contain any version; whole path will be used as a key for m2m decision cache");
        }
        String key = parsedUri.getHost() + ":" + parsedUri.getPort() + "/" + StringUtils.join(filteredSegments, "/");
        if (parsedUri.getPath().startsWith("/api") && StringUtils.isNotEmpty(serviceName)) {
            key = key + "/" + serviceName;
        }

        return key;
    }

    private static boolean isVersion(final String segment) {
        if (segment.length() < 2 || segment.charAt(0) != 'v')
            return false;
        return segment.substring(1).matches("\\d+");
    }
}

package com.netcracker.cloud.quarkus.logging.manager.runtime.consul;

import com.netcracker.cloud.consul.provider.common.TokenStorage;
import com.netcracker.cloud.quarkus.consul.client.ConsulClient;
import com.netcracker.cloud.quarkus.consul.client.ConsulSourceConfig;
import com.netcracker.cloud.quarkus.consul.client.http.QueryParams;
import com.netcracker.cloud.quarkus.consul.client.http.Response;
import com.netcracker.cloud.quarkus.consul.client.model.GetValue;
import com.netcracker.cloud.quarkus.logging.manager.runtime.updater.event.ConfigUpdatedEvent;
import io.quarkus.arc.Arc;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class ConsulLoggingConfigWatchFactory {

    private static final Logger log = Logger.getLogger(ConsulLoggingConfigWatchFactory.class);

    private final TokenStorage tokenStorage;
    private final ConsulClient consulClient;
    final Map<String, Integer> roots;
    final Map<String, TreeMap<Integer, String>> cache;

    ConsulLoggingConfigWatchFactory(ConsulClient consulClient, TokenStorage tokenStorage,
                                    @ConfigProperty(name = "cloud.microservice.namespace") String namespace,
                                    @ConfigProperty(name = "cloud.microservice.name") String microservice) {
        this.consulClient = consulClient;
        this.tokenStorage = tokenStorage;
        this.cache = new ConcurrentHashMap<>();
        this.roots = Map.of(
                String.format("config/%s/%s/", namespace, "application"), 0,
                String.format("config/%s/%s/", namespace, microservice), 1,
                String.format("logging/%s/%s/", namespace, microservice), 2);
    }

    public void initConsulLoggingWatch(@Observes StartupEvent event, ConsulSourceConfig consulSourceConfig, ConsulLoggingSourceConfig consulLoggingSourceConfig) {
        if (consulLoggingSourceConfig.loggingEnabled()) {
            int waitTime = consulSourceConfig.waitTime();
            int consulRetryTime = consulLoggingSourceConfig.consulRetryTime();
            int onSuccessDelayTime = consulLoggingSourceConfig.consulOnSuccessDelayTime();
            log.info("Init consul logging watch");
            for (String root : roots.keySet()) {
                log.infof("Start watching for Consul properties at '/kv/%s'", root);
                watchConsulLoggingRoot(root, waitTime, consulRetryTime, onSuccessDelayTime, 0);
            }
        }
    }

    protected void watchConsulLoggingRoot(String root, int waitTimeSecs, int consulRetryTimeMs, int onSuccessDelayTimeMs, long index) {
        CompletableFuture.supplyAsync(tokenStorage::get)
                .whenCompleteAsync((String token, Throwable tokenEx) -> {
                    if (tokenEx != null) {
                        log.warnf("Failed to obtain token From TokenStorage. Error: %s. Retrying after %s",
                                tokenEx.getMessage(), Duration.ofMillis(consulRetryTimeMs));
                        CompletableFuture.runAsync(() -> watchConsulLoggingRoot(root, waitTimeSecs, consulRetryTimeMs, onSuccessDelayTimeMs, index),
                                CompletableFuture.delayedExecutor(consulRetryTimeMs, TimeUnit.MILLISECONDS));
                    } else {
                        consulClient.getKVValuesAsync(root, token, new QueryParams(waitTimeSecs, index))
                                .whenCompleteAsync((response, ex) -> {
                                    long retryTimeMs;
                                    if (ex != null) {
                                        retryTimeMs = consulRetryTimeMs;
                                        log.warnf("Error on long polling request to /kv/%s. Error: %s. Retrying after %s", root, ex.getMessage(), Duration.ofMillis(retryTimeMs));
                                    } else {
                                        List<GetValue> values = Optional.ofNullable(response).map(Response::getValue).orElse(null);
                                        if (values != null) {
                                            retryTimeMs = values.isEmpty() ? consulRetryTimeMs : onSuccessDelayTimeMs;
                                            Map<String, String> updatedProperties = mergeProperties(root, values);
                                            log.debugf("Got update at '/kv/%s' with %d updated keys:\n%s",
                                                    root, updatedProperties.size(), String.join("\n", updatedProperties.keySet()));
                                            firePropertiesUpdated(updatedProperties);
                                        } else {
                                            retryTimeMs = consulRetryTimeMs;
                                        }
                                    }
                                    // reschedule next poll
                                    Long indx = Optional.ofNullable(response).map(Response::getConsulIndex).orElse(0L);
                                    log.debugf("Re-schedulling watching for Consul properties at '/kv/%s' with index: '%d' after retryTime: %s", root, indx, Duration.ofMillis(retryTimeMs));
                                    CompletableFuture.runAsync(() -> watchConsulLoggingRoot(root, waitTimeSecs, consulRetryTimeMs, onSuccessDelayTimeMs, indx),
                                            CompletableFuture.delayedExecutor(retryTimeMs, TimeUnit.MILLISECONDS));
                                });
                    }
                });
    }

    protected void firePropertiesUpdated(Map<String, String> properties) {
        if (!properties.isEmpty()) {
            Event<Object> event = Arc.container().beanManager().getEvent();
            event.fire(new ConfigUpdatedEvent(properties, "/"));
        }
    }

    private synchronized Map<String, String> mergeProperties(String root, List<GetValue> kvValues) {
        // cleanup cache (keys could be deleted in consul)
        Integer priority = roots.getOrDefault(root, 0);
        cache.entrySet().stream()
                .filter(e -> {
                    e.getValue().remove(priority);
                    return e.getValue().isEmpty();
                })
                .map(Map.Entry::getKey)
                .toList()
                .forEach(cache::remove);
        Map<String, TreeMap<Integer, String>> merged = Stream.concat(cache.entrySet().stream(), kvValues.stream()
                        .filter(gv -> gv.getValue() != null)
                        .map(gv -> {
                            String key = normalizeLogProperty(gv.getKey(), root);
                            String value = gv.getDecodedValue();
                            return Map.entry(key, new TreeMap<>(Map.of(priority, value)));
                        }))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (v1, v2) -> {
                            v1.putAll(v2);
                            return v1;
                        }));
        cache.putAll(merged);
        return merged.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,
                entry -> entry.getValue().lastEntry().getValue()));
    }

    private String normalizeLogProperty(String key, String root) {
        key = key.substring(root.length()).replace("/", ".");
        String loggingPrefix = "logging.level.";
        if (key.startsWith(loggingPrefix)) {
            key = key.substring(loggingPrefix.length());
            if ("root".equals(key)) {
                key = "quarkus.log.level";
            } else if (!key.startsWith("quarkus.log.category.\"")) {
                key = String.format("quarkus.log.category.\"%s\".level", key);
            }
        }
        return key;
    }
}

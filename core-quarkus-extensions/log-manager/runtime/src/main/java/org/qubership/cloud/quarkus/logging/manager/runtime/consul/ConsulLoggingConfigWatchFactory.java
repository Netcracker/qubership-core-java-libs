package org.qubership.cloud.quarkus.logging.manager.runtime.consul;

import org.qubership.cloud.consul.provider.common.TokenStorage;
import org.qubership.cloud.quarkus.consul.client.ConsulClient;
import org.qubership.cloud.quarkus.consul.client.ConsulSourceConfig;
import org.qubership.cloud.quarkus.consul.client.RetryableConsulClient;
import org.qubership.cloud.quarkus.consul.client.model.GetValue;
import org.qubership.cloud.quarkus.consul.client.http.QueryParams;
import org.qubership.cloud.quarkus.consul.client.http.Response;
import org.qubership.cloud.quarkus.logging.manager.runtime.updater.event.ConfigUpdatedEvent;
import org.qubership.cloud.quarkus.logging.manager.runtime.updater.event.LogUpdateEvent;
import io.quarkus.arc.Arc;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;


public class ConsulLoggingConfigWatchFactory {

    private static final Logger log = Logger.getLogger(ConsulLoggingConfigWatchFactory.class);

    private final TokenStorage tokenStorage;
    private final ConsulClient consulClient;
    final Map<String, Integer> roots;

    record Value(String value, String root) {
    }

    ConsulLoggingConfigWatchFactory(ConsulClient consulClient, TokenStorage tokenStorage,
                                    @ConfigProperty(name = "cloud.microservice.namespace") String namespace,
                                    @ConfigProperty(name = "cloud.microservice.name") String microservice) {
        this.consulClient = consulClient;
        this.tokenStorage = tokenStorage;
        this.roots = Map.of(
                String.format("config/%s/%s/", namespace, "application"), 0,
                String.format("config/%s/%s/", namespace, microservice), 1,
                String.format("logging/%s/%s/", namespace, microservice), 2);
    }

    public void initConsulLoggingWatch(@Observes StartupEvent event, ConsulSourceConfig consulSourceConfig, ConsulLoggingSourceConfig consulLoggingSourceConfig) {
        if (consulLoggingSourceConfig.loggingEnabled()) {
            log.info("Init consul logging watch");
            runConsulLoggingWatcher(consulSourceConfig, consulLoggingSourceConfig);
        }
    }

    protected void runConsulLoggingWatcher(ConsulSourceConfig consulSourceConfig, ConsulLoggingSourceConfig consulLoggingSourceConfig) {
        RetryableConsulClient retryableConsulClient = new RetryableConsulClient(consulClient, tokenStorage);

        Thread watcher = new Thread(() -> {
            String root = "";
            Response<List<GetValue>> kvValuesResponse = retryableConsulClient.callConsulWithRetry(root, consulLoggingSourceConfig.consulRetryTime());
            List<GetValue> values = Optional.ofNullable(kvValuesResponse.getValue()).orElse(Collections.emptyList());
            firePropertiesUpdated(mergeProperties(values));
            log.info("Start watching for Consul properties at '/kv/'");
            long lastIndex = kvValuesResponse.getConsulIndex();
            while (true) {
                try {
                    Response<List<GetValue>> pooledKvValuesResponse = retryableConsulClient.getKVValues(root, new QueryParams(consulSourceConfig.waitTime(), lastIndex));
                    lastIndex = pooledKvValuesResponse.getConsulIndex();
                    if (pooledKvValuesResponse.getValue() != null) {
                        Map<String, String> updatedProperties = mergeProperties(pooledKvValuesResponse.getValue());
                        log.debugv("Got update at '/kv/' with {1} updated items", updatedProperties.size());
                        firePropertiesUpdated(updatedProperties);
                    }
                } catch (Exception ex) {
                    log.error(ex);
                    try {
                        Thread.sleep(Duration.ofSeconds(5).toMillis());
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        });
        watcher.setDaemon(true);
        watcher.setName("consul-properties-watcher");
        watcher.start();
    }

    protected void firePropertiesUpdated(Map<String, String> properties) {
        if (!properties.isEmpty()) {
            Event<Object> event = Arc.container().beanManager().getEvent();
            event.fire(new ConfigUpdatedEvent(properties, "/"));
        }
    }

    private Map<String, String> mergeProperties(List<GetValue> kvValues) {
        return kvValues.stream()
                .filter(v -> v.getValue() != null)
                .map(gv -> {
                    Optional<String> r = roots.keySet().stream().filter(gv.getKey()::startsWith).findAny();
                    if (r.isEmpty()) {
                        return null;
                    } else {
                        String root = r.get();
                        String key = normalizeLogProperty(gv, root);
                        String value = gv.getDecodedValue();
                        return new AbstractMap.SimpleEntry<>(key, new Value(value, root));
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue,
                        (v1, v2) -> roots.get(v1.root) > roots.get(v2.root) ? v1 : v2))
                .entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().value));
    }

    private String normalizeLogProperty(GetValue gv, String root) {
        String key = gv.getKey().substring(root.length()).replace("/", ".");
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
